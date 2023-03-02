package com.transgate.api.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

class TaskHelper4 extends TimerTask {
//    app/crons/autopassdisputesforsettlement
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
        executeGet("app/crons/cards/disputes/update-dispute-data");
    }
}

class TaskHelper3 extends TimerTask {
//    app/crons/autopassdisputesforsettlement
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
        executeGet("app/crons/cards/disputes/update-nuban");
    }
}

class TaskHelper2 extends TimerTask {
//    app/crons/autopassdisputesforsettlement
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
        executeGet("app/crons/autopassdisputesforsettlement");
    }
}

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
    static TimerTask taskHelper2 = new TaskHelper2();
    static TimerTask taskHelper3 = new TaskHelper3();
    static TimerTask taskHelper4 = new TaskHelper4();
    
    @Bean
    public RestTemplate handleRestTemplate() {
        return new RestTemplate();
    }

    public static void main(String[] args) throws JSONException {
//    Validators validators = new Validators();
//    RestCall restCall = new RestCall();
        timer.scheduleAtFixedRate(taskHelper, 120000, 120000); //run at every 2mins
        timer.scheduleAtFixedRate(taskHelper2, 3600000, 3600000);  //run at every 1 hr
        timer.scheduleAtFixedRate(taskHelper3, 5000, 5000);  //run at every 5 secs
//        timer.scheduleAtFixedRate(taskHelper4, 2000, 5000);
//        String[]  numbs = {"561061615001005900", "391039717001005900", "391038944201005900", "249024167801005900", "201012965601501100",
//        "235027216301005900", "395035603701005900", "221085084001005900", "248029048901005900", "504074614601005900"};
//        
//        for (int i = 0; i < numbs.length; i++) {
//            System.out.println(numbs[i] + " formats to "+validators.FormatCardHolderAcctNum(numbs[i]) + " NUBAN => " + restCall.getNuban(validators.FormatCardHolderAcctNum(numbs[i])));
//        }
        SpringApplication.run(TransgateApi.class, args);
    }
    
}
