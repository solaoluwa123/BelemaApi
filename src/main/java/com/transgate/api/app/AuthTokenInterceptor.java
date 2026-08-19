
package com.transgate.api.app;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transgate.api.app.services.Validators;
import com.transgate.api.audit.AuditConstants;
import com.transgate.api.util.PlatformRole;
import com.transgate.api.util.ResponseManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    ResponseManager responseManager = new ResponseManager();
    
    // Constructor injection for RestCall
    public AuthTokenInterceptor(Validators validators) {
        this.validators = validators;
    }
    // List of excluded endpoints
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "/sparkpayapi/app/crons/autopassdisputesforsettlement",
        "/sparkpayapi/app/crons/autopassarbitrateddisputesforsettlement",
        "/sparkpayapi/users/crons/reducelocktime",
        "/sparkpayapi/users/crons/unlock",
        "/sparkpayapi/app/crons/cards/disputes/update-nuban",
        "/sparkpayapi/app/crons/cards/disputes/update-nuban",
        "/sparkpayapi/users/login",
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
    
    // Path prefixes that do not require auth (e.g. Swagger UI and OpenAPI docs)
    private static final List<String> EXCLUDED_PATH_PREFIXES = Arrays.asList(
        "/sparkpayapi/swagger-ui",
        "/sparkpayapi/v3/api-docs"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        // Skip validation if the endpoint is excluded (exact match)
        if (EXCLUDED_PATHS.contains(requestPath)) {
            return true; // Allow the request through without validation
        }
        // Skip validation for Swagger UI and OpenAPI docs (prefix match)
        for (String prefix : EXCLUDED_PATH_PREFIXES) {
            if (requestPath.startsWith(prefix)) {
                return true;
            }
        }
        
        String whichToken = "auth-token";
        String authToken = request.getHeader("auth-token");
        if (authToken == null || authToken.isEmpty()) {
            authToken = request.getHeader("authorization");
            whichToken = "authorization";
        }
        // Check if auth-token is present and valid (you can add more complex logic here)
        if (authToken == null || authToken.isEmpty() || !validators.ValidateJSONWebToken(authToken)) {
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.getWriter().write(authToken);
            writeResponse(response, responseManager.ResponseUnathorized());
            return false;
        }
        
        int role = -1;
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
            request.setAttribute(AuditConstants.ATTR_ACTOR_USERNAME, username == null ? null : String.valueOf(username));
            request.setAttribute(AuditConstants.ATTR_ACTOR_EMAIL, email == null ? null : String.valueOf(email));
            request.setAttribute(AuditConstants.ATTR_ACTOR_ROLE, role);
        } catch(IOException | DataAccessException | NumberFormatException e) {
            writeResponse(response, responseManager.InvalidSession());
            return false;
        }

        if (PlatformRole.isReadOnly(role) && isBlockedMutation(request, requestPath)) {
            writeResponse(response, responseManager.ResponseForbidden("Read-only users cannot modify data"));
            return false;
        }

        // Allow the request to proceed if the token is valid
        return true;
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
