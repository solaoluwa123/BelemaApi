/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.transgate.api.util;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.MediaType;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
/**
 *
 * @author Makintola
 */

public class Mailers {
    HabariMailEncrypt habariMailEncrypt = new HabariMailEncrypt();
    public String executeRequest(String fullURL, String methodType, String dataToSend, String AuthorizationToken, String mailAuth) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(fullURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(methodType);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty ("Authorization", AuthorizationToken);
            connection.setRequestProperty("AUTH", mailAuth);
            connection.setDoOutput(true);
//            System.out.println("dataToSend: " + dataToSend);
            try (OutputStream wr = connection.getOutputStream()){
                byte[] in = dataToSend.getBytes("utf-8");
                wr.write(in, 0, in.length);
            }
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder myResponse = new StringBuilder();
            String my_response;
            while ((my_response = rd.readLine()) != null) {
                myResponse.append(my_response);
            }
            connection.disconnect();
//            System.out.println("myResponse: " + myResponse.toString());
            return myResponse.toString();
        } catch (IOException e) {
            System.out.println(e.toString());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    public static void sendMail(String toEmail, String subject, String body, String fromEmail, String ccEmail) {
        // Set up JavaMail properties
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", 587);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        // Create a Session with the email and password for authentication
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("supersofttechltd2023@gmail.com", "lqgjbhcfaofhizgd");
            }
        });

        try {
//            Address[] address = [];
            // Create a MimeMessage
            Message message = new MimeMessage(session);

            // Set the sender and recipient
            message.setFrom(new InternetAddress(fromEmail));
            message.setReplyTo(new InternetAddress[] { new InternetAddress(fromEmail) });
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            if (!ccEmail.equals("")) {
                message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccEmail));
            }
            // Set the email subject and text
            message.setSubject(subject);
//            message.addFrom
            message.setContent(body, "text/html");

            // Send the email
            Transport.send(message);
            
            System.out.println("Mail sent to " + toEmail);
        } catch (MessagingException e) {
            System.out.println("Mail error: " + e.toString());
        }
    }
    
    public String SendMailWithHabari(String subject, String sender, String recipient, String message) {
        HttpURLConnection connection = null;
        try {
            String location = "https://habaripay.gtbank.com/api/message/email";
            String bearerToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiMSIsInJvbGUiOiIxIiwibmJmIjoxNjI5OTExMTU5LCJleHAiOjE2Mjk5MjE5NTksImlhdCI6MTYyOTkxMTE1OX0.b4xSxmT8LhL9EueJul3SyRr-46UG62_3XozG6ws_KkY";
            String encryptSender = habariMailEncrypt.encryptText(sender);
            String encryptRecipient = habariMailEncrypt.encryptText(recipient);
            String encryptSubject = habariMailEncrypt.encryptText(subject);
            String encryptMessage = habariMailEncrypt.encryptText(message);
            String clientId = "9";
            URL url = new URL(location);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
//            connection.setRequestProperty("Content-Type", MediaType.MULTIPART_FORM_DATA_VALUE);
//            connection.setRequestProperty ("Authorization", bearerToken);
            connection.setDoOutput(true);
            
            // Create a query string from the form data
            Map<String, String> form = new HashMap<>();
            form.put("Recipient", encryptRecipient);
            form.put("EmailMessage", encryptMessage);
            form.put("EmailSubject", encryptSubject);
            form.put("ClientId", clientId);
            form.put("Sender", encryptSender);
            System.out.println("form : " + form.toString());
//            String dataToSend = "{\"Recipient\": \""+encryptRecipient+"\",\"EmailMessage\": \""+encryptMessage+"\",\"EmailSubject\": \""+encryptSubject+"\",\"ClientId\": \""+clientId+"\",\"Sender\": \""+encryptSender+"\"}";
//            System.out.println("dataToSend: " + dataToSend);
            try (OutputStream wr = connection.getOutputStream()){
                byte[] in = form.toString().getBytes("utf-8");
                connection.setRequestProperty("Content-Length", String.valueOf(in.length));
                wr.write(in, 0, in.length);
            }
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder myResponse = new StringBuilder();
            String my_response;
            while ((my_response = rd.readLine()) != null) {
                myResponse.append(my_response);
            }
            System.out.println("myResponse: " + myResponse.toString());
            return myResponse.toString();
        } catch (IOException e) {
            System.out.println(e.toString());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    public String SendMailWithHabariOkHttpClient(String subject, String sender, String recipient, String message, String attachmentName, String attachmentPath) {
        String location = "https://habaripay.gtbank.com/api/message/email";
        String bearerToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiMSIsInJvbGUiOiIxIiwibmJmIjoxNjI5OTExMTU5LCJleHAiOjE2Mjk5MjE5NTksImlhdCI6MTYyOTkxMTE1OX0.b4xSxmT8LhL9EueJul3SyRr-46UG62_3XozG6ws_KkY";
        String encryptSender = habariMailEncrypt.encryptText(sender);
        String encryptRecipient = habariMailEncrypt.encryptText(recipient);
        String encryptSubject = habariMailEncrypt.encryptText(subject);
        String encryptMessage = habariMailEncrypt.encryptText(message);
        String encryptAttachmentName = habariMailEncrypt.encryptText(attachmentName);
//        String encryptAttachmentPath = habariMailEncrypt.encryptText(attachmentPath);
        String clientId = "9";
        
        OkHttpClient client = new OkHttpClient();
        
        MediaType mediaType = MediaType.parse("multipart/form-data");
        RequestBody body = new MultipartBuilder()
//        RequestBody body = new MultipartBuilder.Builder().setType(MultipartBody.FORM)
          .addFormDataPart("Recipient", encryptRecipient)
          .addFormDataPart("EmailMessage", encryptMessage)
          .addFormDataPart("EmailSubject", encryptSubject)
          .addFormDataPart("ClientId", clientId)
          .addFormDataPart("Sender", encryptSender)
          .addFormDataPart("AttachmentName", encryptAttachmentName)
          .addFormDataPart("Attachment", attachmentPath, RequestBody.create(MediaType.parse("application/octet-stream"),
                new File(attachmentPath)))
          .type(mediaType)
          .build();
        Request request = new Request.Builder()
          .url(location)
          .addHeader("Authorization", bearerToken)
          .addHeader("Content-Type", body.contentType().toString())
          .method("POST", body)
          .build();
        try {
            Response response = client.newCall(request).execute();
            return response.toString();
        } catch (IOException ex) {
            Logger.getLogger(Mailers.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
    public String SendMailWithHabariOkHttpClient(String subject, String sender, String recipient, String message) {
        String location = "https://habaripay.gtbank.com/api/message/email";
        String bearerToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1lIjoiMSIsInJvbGUiOiIxIiwibmJmIjoxNjI5OTExMTU5LCJleHAiOjE2Mjk5MjE5NTksImlhdCI6MTYyOTkxMTE1OX0.b4xSxmT8LhL9EueJul3SyRr-46UG62_3XozG6ws_KkY";
        String encryptSender = habariMailEncrypt.encryptText(sender);
        String encryptRecipient = habariMailEncrypt.encryptText(recipient);
        String encryptSubject = habariMailEncrypt.encryptText(subject);
        String encryptMessage = habariMailEncrypt.encryptText(message);
//        String encryptAttachmentPath = habariMailEncrypt.encryptText(attachmentPath);
        String clientId = "9";
        
        OkHttpClient client = new OkHttpClient();
        
        MediaType mediaType = MediaType.parse("multipart/form-data");
        RequestBody body = new MultipartBuilder()
//        RequestBody body = new MultipartBuilder.Builder().setType(MultipartBody.FORM)
          .addFormDataPart("Recipient", encryptRecipient)
          .addFormDataPart("EmailMessage", encryptMessage)
          .addFormDataPart("EmailSubject", encryptSubject)
          .addFormDataPart("ClientId", clientId)
          .addFormDataPart("Sender", encryptSender)
          .type(mediaType)
          .build();
        Request request = new Request.Builder()
          .url(location)
          .addHeader("Authorization", bearerToken)
          .addHeader("Content-Type", body.contentType().toString())
          .method("POST", body)
          .build();
        try {
            Response response = client.newCall(request).execute();
            return response.toString();
        } catch (IOException ex) {
            Logger.getLogger(Mailers.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
}
