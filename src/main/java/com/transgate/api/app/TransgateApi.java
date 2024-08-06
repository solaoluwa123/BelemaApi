package com.transgate.api.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class TransgateApi //extends SpringBootServletInitializer 
{
    
    @Bean
    public RestTemplate handleRestTemplate() {
        return new RestTemplate();
    }

    private static final String BASEURI = "http://localhost:82/sparkpayapi";
    
    public static void executeGet(String targetURL) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(BASEURI + "/" + targetURL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder myResponse = new StringBuilder();
            String my_response;
            while ((my_response = rd.readLine()) != null) {
                myResponse.append(my_response);
            }
        } catch (IOException e) {
            System.out.println(e.toString());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    public static void main(String[] args){
        SpringApplication.run(TransgateApi.class, args);
    }
    
    @Scheduled(fixedRate = 1000 * 60) // Run every minute (10000 milliseconds)
    public void unlockUsers() {
        executeGet("users/crons/reducelocktime");
        executeGet("users/crons/unlock");
    }
    
    @Scheduled(fixedRate = 1000 * 60 * 60 * 24) // Run every day (10000 milliseconds)
    public void acceptDisputes() {
        executeGet("app/crons/autopassdisputesforsettlement");
    }
    
    @Scheduled(fixedRate = 1000 * 120) // Run every 2 minutes (10000 milliseconds)
    public void updateOldAccountNUBAN() {
        executeGet("app/crons/cards/disputes/update-nuban");
    }
    
//    @Scheduled(fixedRate = 10000) // Run every day (10000 milliseconds)
    @Scheduled(cron = "0 0 8 * * *") // Run at 8 AM every day
    public void acceptedDisputesTaskHelper() {
        executeGet("app/crons/sendaccepteddisputes");
    }
    
    @Scheduled(cron = "0 0 8,12,16 * * *") //Run 3 times daily - 8am, 12pm and 4pm
    public void disputesRemindersTaskHelper() {
        executeGet("app/crons/senddisputesreminders");
    }
    
}
