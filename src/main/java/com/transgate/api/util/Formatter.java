/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.transgate.api.util;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Makintola
 */
public class Formatter {
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
