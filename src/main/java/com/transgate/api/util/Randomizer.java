/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.util;

import de.taimos.totp.TOTP;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Random;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;

/**
 *
 * @author Makintola
 */
public class Randomizer {
    static GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
    public static String GenerateWalletNumber() {
        Random rng = new Random();
        String characters = "0123456789";
        int length = 10;
        
        char[] text = new char[length];
        for (int i = 0; i < length; i++){
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
    
    public String GenerateReference(int length, String string) {
        Random rng = new Random();
        String characters = string;
        
        char[] text = new char[length];
        for (int i = 0; i < length; i++){
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
    
    public String GenerateReference() {
        return GenerateReference(10, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }
    
    public String GenerateToken() {
        return GenerateReference(6, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0987654321!@#$%^&*()_+=-{}");
    }
    
    public String GenerateToken(int length) {
        return GenerateReference(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0987654321!@#$%^&*()_+=-{}");
    }
            
    public static String GeneratePassword() {
        Random rng = new Random();
        String characters = "0123456789abcdefghijklmnopqrstuvwxyz@$-+";
        int length = 8;
        
        char[] text = new char[length];
        for (int i = 0; i < length; i++){
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }
    
    public static String GenerateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }
    
    public static String GetTOTPCode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        return TOTP.getOTP(hexKey);
    }
    
    public static String GetGoogleAuthenticatorBarCode(String secretKey, String account, String issuer) {
    try {
        return "otpauth://totp/"
                + URLEncoder.encode(issuer + ":" + account, "UTF-8").replace("+", "%20")
                + "?secret=" + URLEncoder.encode(secretKey, "UTF-8").replace("+", "%20")
                + "&issuer=" + URLEncoder.encode(issuer, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static GoogleAuthenticatorKey GenerateGoogleAuthenticatorSecretKey() {
//        GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key;
    }
    
    public static String GetGoogleAuthenticatorQRCode(String issuer, String account, GoogleAuthenticatorKey key) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(issuer, account, key);
    }
    
    public static boolean AuthorizeGoogleAuthenticatorCode(String secret, int code) {
//        GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
        return googleAuthenticator.authorize(secret, code);
    }
}
