/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Makintola
 */

public class RestCall {
    
    public String getNuban(String accountNumber) throws JSONException {
        HttpURLConnection connection = null;
        String nuban = "";
        try {
            URL url = new URL("https://habaripay.gtbank.com/bank-services/v1/retrieve-nuban?accountNumber="+accountNumber);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty ("api-key", "hdst6-5sduy-89y42-nk89dafa");
            connection.setDoOutput(true);
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder myResponse = new StringBuilder();
            String my_response;
            while ((my_response = rd.readLine()) != null) {
                myResponse.append(my_response);
            }
            JSONObject response = new JSONObject(myResponse.toString());
            nuban = response.getBoolean("success") ? response.getJSONObject("data").getString("nuban") : "";
        } catch (IOException e) {
            System.out.println(e.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            return nuban;
        }
    }
}
