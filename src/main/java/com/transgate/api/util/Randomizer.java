/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.util;

import java.util.Random;

/**
 *
 * @author Makintola
 */
public class Randomizer {
    
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
}
