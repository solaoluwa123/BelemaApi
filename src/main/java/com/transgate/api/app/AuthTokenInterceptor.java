
package com.transgate.api.app;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.transgate.api.app.services.Validators;
import com.transgate.api.util.ResponseManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
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
        "/sparkpayapi/users/resetpassword",
        "/sparkpayapi/user/generate-token"
    );
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        // Skip validation if the endpoint is excluded
        if (EXCLUDED_PATHS.contains(requestPath)) {
            return true; // Allow the request through without validation
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
        
        try {
            String SQL = "SELECT role FROM tbl_user_details WHERE session_token = ?";
            if (whichToken.equals("authorization") && !authToken.isEmpty() && authToken != null) {
                return true;
            }
            int role = jdbcTemplate.queryForObject(SQL, new Object[]{authToken}, int.class);
            if (role < 1) {
                writeResponse(response, responseManager.InvalidSession());
                return false;
            }
        } catch(IOException | DataAccessException e) {
            writeResponse(response, responseManager.InvalidSession());
            return false;
        }

        // Allow the request to proceed if the token is valid
        return true;
    }
    
    private void writeResponse(HttpServletResponse response, ResponseEntity<String> entity) throws IOException {
        response.setStatus(entity.getStatusCodeValue());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(entity.getBody()));
    }
   
}
