
package com.transgate.api.app;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transgate.api.app.services.AppEnvironmentConfig;
import com.transgate.api.app.services.Validators;
import com.transgate.api.audit.AuditConstants;
import com.transgate.api.util.PlatformRole;
import com.transgate.api.util.ResponseManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
/**
 *
 * @author Makintola
 */
@Component
public class AuthTokenInterceptor implements HandlerInterceptor {

    @Autowired
    @Qualifier("jdbcTemplate")
    JdbcTemplate jdbcTemplate;
    
    private final Validators validators;
    private final AppEnvironmentConfig appConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    ResponseManager responseManager = new ResponseManager();
    
    public AuthTokenInterceptor(Validators validators, AppEnvironmentConfig appConfig) {
        this.validators = validators;
        this.appConfig = appConfig;
    }

    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/sparkpayapi/app/crons/autopassdisputesforsettlement",
        "/sparkpayapi/app/crons/autopassarbitrateddisputesforsettlement",
        "/sparkpayapi/users/crons/reducelocktime",
        "/sparkpayapi/users/crons/unlock",
        "/sparkpayapi/app/crons/cards/disputes/update-nuban",
        "/sparkpayapi/app/crons/cards/disputes/update-nuban",
        "/sparkpayapi/users/login",
        "/sparkpayapi/users/login-2fa",
        "/sparkpayapi/users/recoverpassword",
        "/sparkpayapi/users/resetpassword",
        "/sparkpayapi/users/activateaccount",
        "/sparkpayapi/user/generate-token"
    );

    /** POST/PUT/DELETE/PATCH paths still allowed for read-only sessions. */
    private static final List<String> READ_ONLY_MUTATION_ALLOWLIST = Arrays.asList(
        "/sparkpayapi/users/logout",
        "/sparkpayapi/users/update-password",
        "/sparkpayapi/users/login-2fa",
        "/sparkpayapi/users/setup-2fa"
    );

    private static final String PATH_LOGOUT = "/sparkpayapi/users/logout";
    private static final String PATH_UPDATE_PASSWORD = "/sparkpayapi/users/update-password";
    private static final String PATH_SETUP_2FA = "/sparkpayapi/users/setup-2fa";
    
    private static final List<String> EXCLUDED_PATH_PREFIXES = Arrays.asList(
        "/sparkpayapi/swagger-ui",
        "/sparkpayapi/v3/api-docs"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        if (EXCLUDED_PATHS.contains(requestPath)) {
            return true;
        }
        for (String prefix : EXCLUDED_PATH_PREFIXES) {
            if (requestPath.startsWith(prefix)) {
                return true;
            }
        }
        
        String whichToken = "auth-token";
        String authToken = request.getHeader("auth-token");
        if (authToken == null || authToken.isEmpty()) {
            authToken = request.getParameter("auth-token");
        }
        if (authToken == null || authToken.isEmpty()) {
            authToken = request.getHeader("authorization");
            whichToken = "authorization";
        }
        if (authToken == null || authToken.isEmpty() || !validators.ValidateJSONWebToken(authToken)) {
            writeResponse(response, responseManager.ResponseUnathorized());
            return false;
        }
        
        int role = -1;
        String emailAddress = null;
        try {
            if (whichToken.equals("authorization") && !authToken.isEmpty() && authToken != null) {
                return true;
            }
            String SQL = "SELECT username, email_address, role FROM tbl_user_details WHERE session_token = ? AND deleted = 0";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(SQL, new Object[]{authToken});
            if (rows.isEmpty()) {
                writeResponse(response, responseManager.InvalidSession());
                return false;
            }
            Map<String, Object> user = rows.get(0);
            Object roleObj = user.get("role");
            role = roleObj instanceof Number ? ((Number) roleObj).intValue() : Integer.parseInt(String.valueOf(roleObj));
            if (role < 1) {
                writeResponse(response, responseManager.InvalidSession());
                return false;
            }
            Object username = user.get("username");
            Object email = user.get("email_address");
            emailAddress = email == null ? null : String.valueOf(email);
            request.setAttribute(AuditConstants.ATTR_ACTOR_USERNAME, username == null ? null : String.valueOf(username));
            request.setAttribute(AuditConstants.ATTR_ACTOR_EMAIL, emailAddress);
            request.setAttribute(AuditConstants.ATTR_ACTOR_ROLE, role);
        } catch(IOException | DataAccessException | NumberFormatException e) {
            writeResponse(response, responseManager.InvalidSession());
            return false;
        }

        if (PlatformRole.isReadOnly(role) && isBlockedMutation(request, requestPath)) {
            writeResponse(response, responseManager.ResponseForbidden("Read-only users cannot modify data"));
            return false;
        }

        if (!enforceOnboardingGates(requestPath, emailAddress, response)) {
            return false;
        }

        return true;
    }

    /**
     * When require-password-change / require-2fa are on, restrict the session to
     * setup endpoints until the user finishes those steps.
     */
    private boolean enforceOnboardingGates(String requestPath, String emailAddress, HttpServletResponse response)
            throws IOException {
        if (emailAddress == null || emailAddress.isEmpty()) {
            return true;
        }
        if (!appConfig.isRequirePasswordChange() && !appConfig.isRequire2fa()) {
            return true;
        }
        int mustChange = 0;
        int twoFaEnabled = 0;
        try {
            List<Map<String, Object>> creds = jdbcTemplate.queryForList(
                    "SELECT must_change_password, two_fa_enabled FROM sparkpayweb_db.tbl_users WHERE username = ?",
                    new Object[]{emailAddress});
            if (creds.isEmpty()) {
                return true;
            }
            mustChange = asIntFlag(creds.get(0).get("must_change_password"));
            twoFaEnabled = asIntFlag(creds.get(0).get("two_fa_enabled"));
        } catch (DataAccessException ex) {
            return true;
        }

        boolean needsPasswordChange = appConfig.isRequirePasswordChange() && mustChange == 1;
        boolean needs2faSetup = appConfig.isRequire2fa() && twoFaEnabled == 0;
        if (!needsPasswordChange && !needs2faSetup) {
            return true;
        }

        Set<String> allow = new HashSet<>();
        allow.add(PATH_LOGOUT);
        if (needsPasswordChange) {
            allow.add(PATH_UPDATE_PASSWORD);
        }
        if (needs2faSetup) {
            allow.add(PATH_SETUP_2FA);
            // Still allow password change while 2FA is pending (combined onboarding).
            allow.add(PATH_UPDATE_PASSWORD);
        }
        if (allow.contains(requestPath)) {
            return true;
        }

        String message = needsPasswordChange ? "Password change required" : "2FA setup required";
        writeResponse(response, responseManager.ResponseForbidden(message));
        return false;
    }

    private static int asIntFlag(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private boolean isBlockedMutation(HttpServletRequest request, String requestPath) {
        String method = request.getMethod();
        if (method == null) {
            return false;
        }
        switch (method.toUpperCase(Locale.ROOT)) {
            case "POST":
            case "PUT":
            case "DELETE":
            case "PATCH":
                return !READ_ONLY_MUTATION_ALLOWLIST.contains(requestPath);
            default:
                return false;
        }
    }
    
    private void writeResponse(HttpServletResponse response, ResponseEntity entity) throws IOException {
        response.setStatus(entity.getStatusCodeValue());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(entity.getBody()));
    }
   
}
