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
                    System.out.println("IN");
                    revision = new Date(now.getTime() + dayToMilis * difference);

                    //Curent day is THURS/FRI/SAT
                }else if (difference < 0){
                    System.out.println("IN2");
                    revision = new Date(now.getTime() + dayToMilis * (7 + difference));
                }
            
            }else{
                revision = new Date(now.getTime() + dayToMilis);
            }
            
            
            reply = MachineStatusReply.newBuilder().setWorks(works).setDescription(description).setDate(revision.getDay() + "/" + revision.getMonth() + "/" + revision.getYear() + " at 12am.").build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

    }
}

