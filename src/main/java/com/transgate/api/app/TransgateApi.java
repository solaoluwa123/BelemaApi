package com.transgate.api.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

class TaskHelper extends TimerTask {
    private static final String BASEURI = "http://localhost:82/sparkpayapi";
    
    public void executeGet(String targetURL) {
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
    
    @Override
    public void run() {
        executeGet("users/crons/reducelocktime");
        executeGet("users/crons/unlock");
    }

}

@SpringBootApplication
public class TransgateApi //extends SpringBootServletInitializer 
{
    
//    public SparkpayApplication() {
//    super();
//    setRegisterErrorPageFilter(false); // <- this one
//}
    

//    @Override
//    protected SpringApplicationBuilder configure(SpringApplicationBuilder application)
//    {
//        return application.sources(SparkpayApplication.class);
//    }
    
    static Timer timer = new Timer();
    
    static TimerTask taskHelper = new TaskHelper();
    
    @Bean
    public RestTemplate handleRestTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) {
        timer.scheduleAtFixedRate(taskHelper, 120000, 120000);
        SpringApplication.run(TransgateApi.class, args);
    }
    
}
