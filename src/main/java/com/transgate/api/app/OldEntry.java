//package com.transgate.api.app;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.Calendar;
//import java.util.Timer;
//import java.util.TimerTask;
//import org.springframework.boot.SpringApplication;
//import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.context.annotation.Bean;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.web.client.RestTemplate;
//
////class TaskHelper4 extends TimerTask {
//////    app/crons/autopassdisputesforsettlement
////    private static final String BASEURI = "http://localhost:82/sparkpayapi";
////    
////    public void executeGet(String targetURL) {
////        HttpURLConnection connection = null;
////        try {
////            URL url = new URL(BASEURI + "/" + targetURL);
////            connection = (HttpURLConnection) url.openConnection();
////            connection.setDoOutput(true);
////            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
////            StringBuilder myResponse = new StringBuilder();
////            String my_response;
////            while ((my_response = rd.readLine()) != null) {
////                myResponse.append(my_response);
////            }
////        } catch (IOException e) {
////            System.out.println(e.toString());
////        } finally {
////            if (connection != null) {
////                connection.disconnect();
////            }
////        }
////    }
////    
////    @Override
////    public void run() {
////        executeGet("app/crons/cards/disputes/update-dispute-data");
////    }
////}
//
//class TaskHelper3 extends TimerTask {
////    app/crons/autopassdisputesforsettlement
//    private static final String BASEURI = "http://localhost:82/sparkpayapi";
//    
//    public void executeGet(String targetURL) {
//        HttpURLConnection connection = null;
//        try {
//            URL url = new URL(BASEURI + "/" + targetURL);
//            connection = (HttpURLConnection) url.openConnection();
//            connection.setDoOutput(true);
//            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            StringBuilder myResponse = new StringBuilder();
//            String my_response;
//            while ((my_response = rd.readLine()) != null) {
//                myResponse.append(my_response);
//            }
//        } catch (IOException e) {
//            System.out.println(e.toString());
//        } finally {
//            if (connection != null) {
//                connection.disconnect();
//            }
//        }
//    }
//    
//    @Override
//    public void run() {
//        executeGet("app/crons/cards/disputes/update-nuban");
//    }
//}
//
//class TaskHelper2 extends TimerTask {
////    app/crons/autopassdisputesforsettlement
//    private static final String BASEURI = "http://localhost:82/sparkpayapi";
//    
//    public void executeGet(String targetURL) {
//        HttpURLConnection connection = null;
//        try {
//            URL url = new URL(BASEURI + "/" + targetURL);
//            connection = (HttpURLConnection) url.openConnection();
//            connection.setDoOutput(true);
//            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            StringBuilder myResponse = new StringBuilder();
//            String my_response;
//            while ((my_response = rd.readLine()) != null) {
//                myResponse.append(my_response);
//            }
//        } catch (IOException e) {
//            System.out.println(e.toString());
//        } finally {
//            if (connection != null) {
//                connection.disconnect();
//            }
//        }
//    }
//    
//    @Override
//    public void run() {
//        executeGet("app/crons/autopassdisputesforsettlement");
//    }
//}
//
//class TaskHelper extends TimerTask {
//    private static final String BASEURI = "http://localhost:82/sparkpayapi";
//    public void executeGet(String targetURL) {
//        HttpURLConnection connection = null;
//        try {
//            URL url = new URL(BASEURI + "/" + targetURL);
//            connection = (HttpURLConnection) url.openConnection();
//            connection.setDoOutput(true);
//            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            StringBuilder myResponse = new StringBuilder();
//            String my_response;
//            while ((my_response = rd.readLine()) != null) {
//                myResponse.append(my_response);
//            }
//        } catch (IOException e) {
//            System.out.println(e.toString());
//        } finally {
//            if (connection != null) {
//                connection.disconnect();
//            }
//        }
//    }
//    
//    @Override
//    public void run() {
//        executeGet("users/crons/reducelocktime");
//        executeGet("users/crons/unlock");
//    }
//
//}
//
//@SpringBootApplication
//@EnableScheduling
//public class TransgateApi //extends SpringBootServletInitializer 
//{
//    
//    @Bean
//    public RestTemplate handleRestTemplate() {
//        return new RestTemplate();
//    }
//
//    private static final String BASEURI = "http://localhost:82/sparkpayapi";
//    
//    public static String executeGetRes(String targetURL) {
//        HttpURLConnection connection = null;
//        try {
//            URL url = new URL(BASEURI + "/" + targetURL);
//            connection = (HttpURLConnection) url.openConnection();
//            connection.setDoOutput(true);
//            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            StringBuilder myResponse = new StringBuilder();
//            String my_response;
//            while ((my_response = rd.readLine()) != null) {
//                myResponse.append(my_response);
//            }
//            return myResponse.toString();
//        } catch (IOException e) {
//            System.out.println(e.toString());
//            return null;
//        } finally {
//            if (connection != null) {
//                connection.disconnect();
//            }
//        }
//    }
//    
//    public static void executeGet(String targetURL) {
//        HttpURLConnection connection = null;
//        try {
//            URL url = new URL(BASEURI + "/" + targetURL);
//            connection = (HttpURLConnection) url.openConnection();
//            connection.setDoOutput(true);
//            BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            StringBuilder myResponse = new StringBuilder();
//            String my_response;
//            while ((my_response = rd.readLine()) != null) {
//                myResponse.append(my_response);
//            }
//        } catch (IOException e) {
//            System.out.println(e.toString());
//        } finally {
//            if (connection != null) {
//                connection.disconnect();
//            }
//        }
//    }
//    
//    public static void main(String[] args){
//        Timer timer1 = new Timer();
//        Timer timer2 = new Timer();
//        Timer timer3 = new Timer();
//        Timer acceptedDisputesTaskHelper = new Timer();
//        
////        timer1.schedule(new TimerTask() {
////            @Override
////            public void run() {
////                // code to be executed repeatedly
////                executeGet("users/crons/reducelocktime");
////                executeGet("users/crons/unlock");
////            }
////        }, 0, 60000);
//////        
////        timer2.schedule(new TimerTask() {
////            @Override
////            public void run() {
////                // code to be executed repeatedly
////                executeGet("app/crons/autopassdisputesforsettlement");
////            }
////        }, 0, 3600000);
//////        
////        timer3.schedule(new TimerTask() {
////            @Override
////            public void run() {
////                // code to be executed repeatedly
////                executeGet("app/crons/cards/disputes/update-nuban");
////            }
////        }, 0, 10000);
////        
////        Calendar scheduleTime = Calendar.getInstance();
////        scheduleTime.set(Calendar.HOUR_OF_DAY, 8);
////        scheduleTime.set(Calendar.MINUTE, 0);
////        scheduleTime.set(Calendar.SECOND, 0);
////        
////        acceptedDisputesTaskHelper.scheduleAtFixedRate(new TimerTask() {
////            @Override
////            public void run() {
////                System.out.println("Hello world  !");
//////                executeGet("app/crons/sendaccepteddisputes");
////            }
//////        }, scheduleTime.getTime(), 24 * 60 * 60 * 1000);
////        }, 0, 10000);
//        
////    Validators validators = new Validators();
////    RestCall restCall = new RestCall();
////        timer.scheduleAtFixedRate(taskHelper, 0, 60000); //run at every 1min
////        timer.scheduleAtFixedRate(taskHelper2, 0, 3600000);  //run at every 1 hr
////        timer.scheduleAtFixedRate(taskHelper3, 0, 5000);  //run at every 5 secs
////        timer.scheduleAtFixedRate(taskHelper4, 2000, 5000);
////        String[]  numbs = {"561061615001005900", "391039717001005900", "391038944201005900", "249024167801005900", "201012965601501100",
////        "235027216301005900", "395035603701005900", "221085084001005900", "248029048901005900", "504074614601005900"};
////        
////        for (int i = 0; i < numbs.length; i++) {
////            System.out.println(numbs[i] + " formats to "+validators.FormatCardHolderAcctNum(numbs[i]) + " NUBAN => " + restCall.getNuban(validators.FormatCardHolderAcctNum(numbs[i])));
////        }
//        SpringApplication.run(TransgateApi.class, args);
//    }
//    
//    @Scheduled(fixedRate = 10000) // Run every 10 seconds (10000 milliseconds)
//    public void printHelloWorld() {
//        System.out.println("Hello, World!");
//    }
//    
//}
