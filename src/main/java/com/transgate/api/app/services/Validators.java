/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import org.springframework.stereotype.Service;

/**
 *
 * @author Makintola
 */
@Service
public class Validators {
    
    private final AppEnvironmentConfig appConfig;
    public Validators(AppEnvironmentConfig appConfig) {
        this.appConfig = appConfig;
    }
    
    public final String GenerateJSONWebToken(String email, Date expirationDate) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("email", email); // Custom claim
            Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(appConfig.getSecreteV()), 
                            SignatureAlgorithm.HS256.getJcaName());
            Date date = new Date();
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setIssuer(appConfig.getJWTIssuer())
                    .setExpiration(expirationDate)
                    .setIssuedAt(date)
                    .setId(UUID.randomUUID().toString())
                    .setSubject("Habari")
                    .signWith(hmacKey)
                    .compact();
            return token;
        } catch (JwtException e) {
            return "";
        }
    }
    
    public final String GenerateJSONWebToken(String email) {
        try {
            Date date = new Date();
            long time = date.getTime();
            Date expirationDate = new Date(time + (1000 * 60 * 60 * 12)); //12hours
            return GenerateJSONWebToken(email, expirationDate);
//            String token = Jwts.builder()
//                    .setIssuer("Ajijet-x-Habari")
//                    .setExpiration(expirationDate)
//                    .setIssuedAt(date)
//                    .setId(UUID.randomUUID().toString())
//                    .setSubject(email)
//                    .signWith(hmacKey)
//                    .compact();
//            return token;
        } catch (JwtException e) {
            return "";
        }
    }
    
    public final boolean ValidateJSONWebToken(String token, String email) {
        try {
    
            Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(appConfig.getSecreteV()), 
                                    SignatureAlgorithm.HS256.getJcaName());
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(hmacKey).build().parseClaimsJws(token);
            return claims.getBody().get("email", String.class).equals(email) && claims.getBody().getSubject().equals("Habari");
        } catch (JwtException e) {
            return false;
        }
    }
    
    public final boolean ValidateJSONWebToken(String token) {
        try {
    
            Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(appConfig.getSecreteV()), 
                                    SignatureAlgorithm.HS256.getJcaName());
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(hmacKey).build().parseClaimsJws(token);
            return claims.getBody().getSubject().equals("Habari");
        } catch (JwtException e) {
            return false;
        }
    }
    
    
    public final String validHeader() {
        return appConfig.getAPIHeader();
    }
    public final String validHeaderExternal() {
        return appConfig.getAPIHeaderExternal();
    }
}
