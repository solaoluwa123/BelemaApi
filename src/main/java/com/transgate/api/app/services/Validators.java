package com.transgate.api.app.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class Validators {

    private static final Logger logger = LoggerFactory.getLogger(Validators.class);
    private final AppEnvironmentConfig appConfig;

    public Validators(AppEnvironmentConfig appConfig) {
        this.appConfig = appConfig;
    }

    public final String GenerateJSONWebToken(String email, Date expirationDate) {
        try {
            // Validate input parameters.
            if (email == null || email.isEmpty()) {
                logger.info("JWT generation failed: Provided email is null or empty.");
                return "";
            }
            if (expirationDate == null) {
                logger.info("JWT generation failed: Expiration date is null.");
                return "";
            }

            String secretString = appConfig.getSecreteV();
            if (secretString == null || secretString.trim().isEmpty()) {
                logger.info("JWT generation failed: Secret value (appConfig.getSecreteV()) is not configured.");
                return "";
            }

            // Build the claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("email", email); // Custom claim

            // Decode secret from Base64
            Key hmacKey = new SecretKeySpec(
                    Base64.getDecoder().decode(secretString),
                    SignatureAlgorithm.HS256.getJcaName()
            );

            Date now = new Date();

            String token = Jwts.builder()
                    .setClaims(claims)
                    .setIssuer(appConfig.getJWTIssuer() != null ? appConfig.getJWTIssuer() : "")
                    .setExpiration(expirationDate)
                    .setIssuedAt(now)
                    .setId(UUID.randomUUID().toString())
                    .setSubject("Habari")
                    .signWith(hmacKey)
                    .compact();

            return token;
        } catch (JwtException e) {
            logger.info("JWT exception while generating token for email " + email + ": " + e.getMessage());
            return "";
        } catch (Exception e) {
            logger.info("Unexpected error while generating token for email " + email + ": " + e.getMessage());
            return "";
        }
    }

    public final String GenerateJSONWebToken(String email) {
        try {
            Date now = new Date();
            long time = now.getTime();
            Date expirationDate = new Date(time + (1000 * 60 * 60 * 12)); // 12 hours
            return GenerateJSONWebToken(email, expirationDate);
        } catch (Exception e) {
            logger.info("Error generating default token for email {}: {}", email, e.getMessage(), e);
            return "";
        }
    }

    public final boolean ValidateJSONWebToken(String token, String email) {
        try {
            Key hmacKey = new SecretKeySpec(
                    Base64.getDecoder().decode(appConfig.getSecreteV()),
                    SignatureAlgorithm.HS256.getJcaName()
            );
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(hmacKey)
                    .build()
                    .parseClaimsJws(token);

            boolean isValid = claims.getBody().get("email", String.class).equals(email)
                    && "Habari".equals(claims.getBody().getSubject());
            return isValid;
        } catch (JwtException e) {
            logger.info("JWT exception while validating token for email {}: {}", email, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.info("Unexpected error while validating token for email {}: {}", email, e.getMessage(), e);
            return false;
        }
    }

//    public final boolean validateJSONWebToken(String token) {
//        logger.info("Token: " + token);
//        if (token == null || token.chars().filter(ch -> ch == '.').count() != 2) {
//            logger.info("Invalid token format: {}", token);
//            return false;
//        }
//        try {
//            Key hmacKey = new SecretKeySpec(
//                    Base64.getDecoder().decode(appConfig.getSecreteV()),
//                    SignatureAlgorithm.HS256.getJcaName()
//            );
//            Jws<Claims> claims = Jwts.parserBuilder()
//                    .setSigningKey(hmacKey)
//                    .build()
//                    .parseClaimsJws(token);
//            return "Habari".equals(claims.getBody().getSubject());
//        } catch (JwtException e) {
//            logger.info("JWT exception while validating token: {}", e.getMessage(), e);
//            return false;
//        } catch (Exception e) {
//            logger.info("Unexpected error while validating token: {}", e.getMessage(), e);
//            return false;
//        }
//    }
    public final boolean ValidateJSONWebToken(String token) {
        boolean isJWTValid = false;
//        if (token == null || token.chars().filter(ch -> ch == '.').count() != 2) {
//            logger.info("Invalid token format: {}", token);
//            return false;
//        }
        try {
            Key hmacKey = new SecretKeySpec(
                    Base64.getDecoder().decode(appConfig.getSecreteV()),
                    SignatureAlgorithm.HS256.getJcaName()
            );
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(hmacKey)
                    .build()
                    .parseClaimsJws(token);
            isJWTValid = "Habari".equals(claims.getBody().getSubject());

        } catch (JwtException e) {
            logger.info("JWT exception while validating token: {}", e.getMessage(), e);
            return isJWTValid;
        } catch (Exception e) {
            logger.info("Unexpected error while validating token: {}", e.getMessage(), e);
            return isJWTValid;
        }
//        logger.info("Validating JWT :: isJWTValid... " + isJWTValid);
        return isJWTValid;
    }

    public final String validHeader() {
        try {
            String header = appConfig.getAPIHeader();
            return header != null ? header : "";
        } catch (Exception e) {
            logger.info("Error retrieving validHeader: {}", e.getMessage(), e);
            return "";
        }
    }

    public final String validHeaderExternal() {
        try {
            return appConfig.getAPIHeaderExternal();
        } catch (Exception e) {
            logger.info("Error retrieving validHeaderExternal: {}", e.getMessage(), e);
            return "";
        }
    }
}
