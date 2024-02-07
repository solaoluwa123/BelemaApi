/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.util;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.security.Key;
import java.text.NumberFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Makintola
 */
public class Validators {
       
    String secret = "sparkpayxbearerfactorajijetjwtscrete2023habari";

    Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(secret), 
                            SignatureAlgorithm.HS256.getJcaName());
    
    public final String GenerateJSONWebToken(String email) {
        try {
            Date date = new Date();
            long time = date.getTime();
            Date expirationDate = new Date(time + 300000l); //5mins
            String token = Jwts.builder()
                    .setIssuer("Ajijet-x-Habari")
                    .setExpiration(expirationDate)
                    .setIssuedAt(date)
                    .setId(UUID.randomUUID().toString())
                    .setSubject(email)
                    .signWith(hmacKey)
                    .compact();
            return token;
        } catch (JwtException e) {
            System.out.println("JwtException: " + e);
            return "";
        }
    }
    
    public final boolean ValidateJSONWebToken(String token, String email) {
        try {
            System.out.println("token: " + token);
            System.out.println("email: " + email);
            return Jwts.parserBuilder().setSigningKey(hmacKey).build().parseClaimsJws(token).getBody().getSubject().equals(email);
        } catch (JwtException e) {
            System.out.println("JwtException: " + e);
            return false;
        }
    }
    
    public final String validHeader() {
        return "Bearer 958455015C7DB0F3CEDD56F8F3E50E94568905B636A4954A478030E2603E8A7758F8843B7A6EDC837CA5C6B57B262FDF3B44C7FF706DC3EB991EECFC7840FEC7";
    }
    public final String validHeaderExternal() {
        return "x-bearer-factor";
    }
    
    public int removeLeadingZero(String str) {
        return Integer.parseInt(str);
//        if (str.substring(0, 1).equals("0"))
//            return str.substring(1, str.length());
//        else
//            return str;
    }
    
    public String FormatCardHolderAcctNum(String cardHolderAcctNum){
        String formatCardHolderAcctNum = cardHolderAcctNum;
        try {
            String path_1 = cardHolderAcctNum.substring(0, 3);
            String path_2 = cardHolderAcctNum.substring(3, 10);
            String path_3 = cardHolderAcctNum.substring(10, 12);
            String path_4 = cardHolderAcctNum.substring(12, 16);
            String path_5 = cardHolderAcctNum.substring(16, cardHolderAcctNum.length());
            formatCardHolderAcctNum = removeLeadingZero(path_1) + "/" + removeLeadingZero(path_2) + "/" + removeLeadingZero(path_3) + "/" + removeLeadingZero(path_4) + "/" + removeLeadingZero(path_5);
//            return formatCardHolderAcctNum;
        } catch (Exception ex) {
            System.out.println("formatter: " + ex);
        }
        return formatCardHolderAcctNum;
    }
    
    public String RemoveSpecialCharacters(String input) {
        // Define a regular expression pattern to match special characters
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9\\s]");
        // Create a Matcher object
        Matcher matcher = pattern.matcher(input);
        // Replace all special characters with an empty string
        String result = matcher.replaceAll("");
        
        Pattern pattern2 = Pattern.compile("\\s");
        Matcher matcher2 = pattern2.matcher(result);
        result = matcher2.replaceAll("");
        
        return result;
    }
    
    public static String DoubleToCurrency(double amount) {
        // Create a NumberFormat instance with the desired locale
        Locale locale = new Locale("en", "US"); // For example, en_US represents the United States locale
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);

        // Format the double value as currency
        String formattedCurrency = currencyFormatter.format(amount);
        formattedCurrency = formattedCurrency.replace("$", "");
        return formattedCurrency;
    }
}
