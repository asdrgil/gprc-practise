/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package iotucm.coffeeservice;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import iotucm.coffeeservice.CapsuleConsumedReply;
import iotucm.coffeeservice.CapsuleConsumedRequest;
import iotucm.coffeeservice.MachineStatusReply;
import iotucm.coffeeservice.MachineStatusRequest;
import iotucm.coffeeservice.CoffeeServerGrpc;
import iotucm.coffeeservice.*;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.Date;

import java.util.HashMap;
import java.util.Map;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

/**
 * Coffee service implementation
 */
public class CoffeeServiceServer {
    private static final Logger logger = Logger.getLogger(CoffeeServiceServer.class.getName());
    private Server server;
    
    //Constants
    public static final int MAXINCIDENCES = 1;
    public static final float [][] MAXVALUE = new float[][]{{50, 65}, {80, 500}};
    public static final String [][] DESCRIPTIONOPTIONS = new String[][]{{"Temperature is fine.", "Temperature warning.", "Temperature exceeded safe values."}, {" Pressure is fine.", " Pressure warning.", " Pressure exceeded safe values."}};
    public final static String DRIVERLAB = "com.mysql.cj.jdbc.Driver";
    public final static String URLLAB = "jdbc:mysql://localhost/dii_p3?&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&user=user&password=password";

    public static final Map<String, Double> COFFEEPRICES = new HashMap<String, Double>()
            {{
                 put("ristretto", 1.0);
                 put("volutto", 1.2);
                 put("latte", 0.8);
            }};

    public static String getHTML(String urlString) throws Exception {
      StringBuilder result = new StringBuilder();
      URL url = new URL(urlString);
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");
      BufferedReader buffer = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      String readLine;
      while ((readLine = buffer.readLine()) != null)
         result.append(readLine);
         
      buffer.close();
      return result.toString();
    }

    public static float getCurrencyExchange(String currency){
        if(currency.equals("EUR") || currency.equals(""))
            return (float) 1;

        try {
            //Query currency exchange between EUR and the given currency at this moment
            String aux = getHTML("https://api.exchangeratesapi.io/latest?symbols=" + currency);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(aux);
            JSONObject jsonObject = (JSONObject) obj;
            aux = jsonObject.get("rates").toString();
            obj = parser.parse(aux);
            jsonObject = (JSONObject) obj;
            return Float.parseFloat(jsonObject.get(currency).toString());
        } catch (Exception e){
            e.printStackTrace();
            return (float) -1;
        }
    }
    
    //Round value to two decimal positions
    public static float getRound2(float value){
        value *= 100;
        value = Math.round(value);
        return value /100;
    }


    private void start(int port) throws IOException {
        /* The port on which the server should run */
        server = ServerBuilder.forPort(port)
        .addService(new CofeeServerImpl())
        .build()
        .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                CoffeeServiceServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
    * Await termination on the main thread since the grpc library uses daemon threads.
    */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
    * Main launches the server from the command line.
    */
    public static void main(String[] args) throws IOException, InterruptedException {
        final CoffeeServiceServer server = new CoffeeServiceServer();
        int port = 50051;
        if (args.length > 0) {
            port=Integer.parseInt(args[0]);
        }
        server.start(port);
        server.blockUntilShutdown();
    }

    static class CofeeServerImpl extends CoffeeServerGrpc.CoffeeServerImplBase {
        static int counter=10;      
        @Override
        public void consumedCapsule(CapsuleConsumedRequest request, StreamObserver<CapsuleConsumedReply> responseObserver) {
            // Never call super methods, otherwise you will get "call is closed" and "unimplemented method" errors

            CapsuleConsumedReply reply = null;
            if (counter>5) {
                reply=CapsuleConsumedReply.newBuilder().setExpectedRemaining(counter).setExpectedProvisionDate("No need, yet").build();        
            } else {
                reply=CapsuleConsumedReply.newBuilder().setExpectedProvisionDate("11 of november of 2019").setExpectedRemaining(counter).build();
                counter=10;
            }
            counter=counter-1;

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void checkMachineStatus(MachineStatusRequest request, StreamObserver<MachineStatusReply> responseObserver) {

            MachineStatusReply reply = null;
            boolean works = true;
            String description = "";
            int interval[] = new int[]{0, 0};
            
            //Date now = new Date();
            //Date expectedDate = new Date(now.getTime() + 3 * 24 * 60 * 60 * 1000);
            
            /*if(maxTemp[1] >= request.getWaterTemp() && request.getWaterTemp() > maxTemp[0]){
                temp = 1;
            }*/
            
            
            //Check if the values are over the top warning ones.
            for(int i = 0; i < MAXVALUE.length; i++){

                float value;
                

                if (i == 0)
                    value = request.getWaterTemp();
                else
                    value = request.getPressure();

                if (value > MAXVALUE[i][0] && value < MAXVALUE[i][1]){
                    interval[i] = 1;

                }else if (value > MAXVALUE[i][1]){
                    interval[i] = 2;
                }
                
                description += DESCRIPTIONOPTIONS[i][interval[i]];
            }
            
            
            
            if(interval[0] + interval[1] > MAXINCIDENCES){
                works = false;
            }
        
            Date now = new Date();
            Date revision = new Date(now.getTime());
        
            //Programmed inspections are on Wednesdays
            int programmedDay = 3;
            long dayToMilis = 60 * 60 * 24 * 1000L;
        
            if(works){
                int difference = programmedDay - revision.getDay() ; 
            
                //Curent day is SUN/MON/TUES
                if(difference > 0){
                    revision = new Date(now.getTime() + dayToMilis * difference);

                //Curent day is THURS/FRI/SAT
                }else if (difference < 0){
                    revision = new Date(now.getTime() + dayToMilis * (7 + difference));
                }
            
            }else{
                revision = new Date(now.getTime() + dayToMilis);
            }
            
            
            reply = MachineStatusReply.newBuilder().setWorks(works).setDescription(description).setDate(revision.getDay() + "/" + revision.getMonth() + "/" + revision.getYear() + " at 12am.").build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }


        @Override
        public void getPrice(MachinePricesRequest request, StreamObserver<MachinePricesReply> responseObserver) {

            MachinePricesReply reply = null;
            String currency = request.getCurrency();
            String output = "The currency " + currency + " is not registered in the API.";
            float currencyValue = getCurrencyExchange(currency);
            
            //If the given currency is registered in the API
            if(currencyValue != -1){
                  
                output = "The list of the product prices in " + currency + " are:\n";
                Connection con = null;

                try {

                    //Query sql database
                    Class.forName(DRIVERLAB).newInstance();
                    con = DriverManager.getConnection(URLLAB);
                    Statement statement = con.createStatement();
                    String selectSql = "SELECT name, price FROM coffee WHERE units > 0";
                    ResultSet resultSet = statement.executeQuery(selectSql);

                    // Print results from select statement
                    while (resultSet.next())
                        output += resultSet.getString(1) + ": " + Float.toString(getRound2(resultSet.getFloat(2)*currencyValue)) + " " + currency + "\n";

                    if (con != null)
                        con.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            reply = MachinePricesReply.newBuilder().setListPrices(output).build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
        
        @Override
        public void buyProduct(MachineBuyProductRequest request, StreamObserver<MachineBuyProductReply> responseObserver) {

            String currency = request.getCurrency();
            String productName = request.getProductName();
            float quantity = request.getQuantity();
            String giftcard = request.getGiftcard();

            MachineBuyProductReply reply = null;
            String change = "There is not coffee left from type " + productName + ". Please, try with another coffee.";
            
            Connection con = null;

            try {

                Class.forName(DRIVERLAB).newInstance();
                con = DriverManager.getConnection(URLLAB);

                Statement statement = con.createStatement();
                String selectSql = "SELECT price FROM coffee WHERE units > 0 AND name = \"" + productName + "\" limit 1";
                ResultSet resultSet = statement.executeQuery(selectSql);
        
                // If there are any units of the expected product
                if (resultSet.next()){
                
                    float productPrice = resultSet.getFloat(1);
                    float moneyLeft = -productPrice;
                    
                    
                    //Pay in cash
                    if(giftcard.length() == 0){
                        productPrice = getRound2(productPrice * getCurrencyExchange(currency));
                        
                        

                    //Pay with gift card
                    }else{
                        selectSql = "SELECT credit FROM giftcard WHERE token = \"" + giftcard + "\" limit 1";
                        resultSet = statement.executeQuery(selectSql);

                        if (resultSet.next()){
                            quantity = resultSet.getFloat(1);
                        }

                    }
                    
                    moneyLeft += quantity;
                    
                    //Check if the user has enough money to buy the product
                    if(moneyLeft >= 0){
                        PreparedStatement pstmt = null;
			            pstmt = con.prepareStatement("UPDATE coffee  SET units = units - 1 WHERE  name=\""+ productName +"\";");
			            pstmt.executeUpdate();
			            pstmt = con.prepareStatement("UPDATE money  SET ammount = ammount + " + Float.toString(productPrice) + "WHERE  currency=\""+ currency +"\";");
			            pstmt.executeUpdate();
			            
			            //Pay with giftcard
			            if(giftcard.length() > 0){
			                pstmt = con.prepareStatement("UPDATE giftcard  SET credit = credit - " + Float.toString(productPrice) + "WHERE  token=\""+ giftcard + "\";");
			                pstmt.executeUpdate();
			            }

                        change = "Here is you coffee of type " + productName + ". The spare money is " + Float.toString(moneyLeft) + currency;
                    
                    } else {
                        change = "The product " + productName + " costs " + Float.toString(productPrice) + ". you have entered " + Float.toString(quantity) + ". Please try again.";
                    }
                }

                if (con != null)
                    con.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            reply = MachineBuyProductReply.newBuilder().setChange(change).build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

    }
}
