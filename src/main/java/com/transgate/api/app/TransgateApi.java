package com.transgate.api.app;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import com.transgate.api.app.controllers.UsersController;
import com.transgate.api.app.services.AppEnvironmentConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private static Logger logger = Logger.getLogger(TransgateApi.class.getName());

    private final AppEnvironmentConfig appConfig;

    // constructor injection (good practice)
    public TransgateApi(AppEnvironmentConfig appConfig) {
        this.appConfig = appConfig;
    }

    private String getBaseUri() {
        return appConfig.getAPIBASEURL();
    }
    private final String getAutoacceptdisputes_1() {
        return appConfig.getAutoacceptdisputes();
    }



//    private  final String BASEURI = "http://localhost:82/sparkpayapi";

    public  void executeGet(String targetURL) {
        long startTime = System.currentTimeMillis();
        String correlationId = java.util.UUID.randomUUID().toString();
        HttpURLConnection connection = null;
        try {
//            logger.info(String.format("1️⃣ Entering executeGet - correlationId=%s - targetURL=%s", correlationId, targetURL));

            URL url = new URL(getBaseUri() + "/" + targetURL);
            logger.info(String.format("2️⃣ Opening connection - correlationId=%s - fullURL=%s", correlationId, getBaseUri() + "/" + targetURL));

            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);

            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder myResponse = new StringBuilder();
            String my_response;
            while ((my_response = rd.readLine()) != null) {
                myResponse.append(my_response);
            }

            long elapsed = System.currentTimeMillis() - startTime;
//            logger.info(String.format("3️⃣ Read complete - correlationId=%s - bytesRead=%d - elapsedMs=%d",
//                    correlationId, myResponse == null ? 0 : myResponse.length(), elapsed));
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            // preserve existing behavior
            System.out.println(e.toString());
            // add structured logs
//            logger.info(String.format("4️⃣ IOException in executeGet - correlationId=%s - targetURL=%s - message=%s - elapsedMs=%d",
//                    correlationId, targetURL, e.getMessage(), elapsed));
//            logger.info(String.format("4️⃣ Exception details - correlationId=%s - exception=%s", correlationId, e.toString()));
        } finally {
            if (connection != null) {
//                logger.info(String.format("5️⃣ Disconnecting connection - correlationId=%s", correlationId));
                connection.disconnect();
            } else {
//                logger.info(String.format("5️⃣ No connection to disconnect - correlationId=%s", correlationId));
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

    @Scheduled(fixedRateString = "${app.autoacceptdisputes}")
    public void acceptDisputes() {
        executeGet("app/crons/autopassdisputesforsettlement");
    }

    @Scheduled(cron = "0 0 2 1/5 * ?")  // Runs at 2 AM every 5 days
    public void acceptArbitratedDisputes() {
        executeGet("app/crons/autopassarbitrateddisputesforsettlement");
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
