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

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.HashMap;
import java.util.Map;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import iotucm.coffeeservice.CapsuleConsumedReply;
import iotucm.coffeeservice.CapsuleConsumedRequest;
import iotucm.coffeeservice.CoffeeServerGrpc;
import iotucm.coffeeservice.*;

/**
 * A simple client that requests a greeting from the {@link CoffeeServerServer}.
 */
public class CoffeeServerClient {
    private static final Logger logger = Logger.getLogger(CoffeeServerClient.class.getName());

    private final ManagedChannel channel;
    private final CoffeeServerGrpc.CoffeeServerBlockingStub blockingStub;

    /** Construct client connecting to HelloWorld server at {@code host:port}. */
    public CoffeeServerClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext()
        .build());
    }

    /** Construct client for accessing HelloWorld server using the existing channel. */
    CoffeeServerClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = CoffeeServerGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    /** A capsule is consumed */
    public void consumeCapsule(String clientid, String type) {
        logger.info("Sending out the consumption of capsule by " + clientid + " of type "+type);
        CapsuleConsumedRequest request = CapsuleConsumedRequest.newBuilder().setClientid(clientid).setType(type).build();
        CapsuleConsumedReply response;
        try {
            response = blockingStub.consumedCapsule(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        if (response.getSupplyProvisioned()!=0)
            logger.info("Result: expect a new deliver by " + response.getExpectedProvisionDate());
        else 
            logger.info("There is still coffee. Expected remaining " + response.getExpectedRemaining());
    }

    /** A capsule is consumed */
    public void checkMachineStatus(float waterTemp, long date, float pressure) {
        MachineStatusRequest request = MachineStatusRequest.newBuilder().setWaterTemp(waterTemp).setDate(date).setPressure(pressure).build();
        MachineStatusReply response;
        try {
            response = blockingStub.checkMachineStatus(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        
        String worksString = "The machine works correctly. ";
        
        if(!response.getWorks()){
            worksString = "The machine does not work. ";
        }
        
        logger.info("Description of the queries: " + response.getDescription() + worksString + "The next inspection will be on " + response.getDate());
    }

    /**
    * Coffee server code. The first argument is the client id, the second, the capsule type, the fourth the server ip, the fifth the port.
    */
    public static void main(String[] args) throws Exception {
        String clientid = "myclientid";
        String capsuletype= "ristretto";
        int port=50051;
        String host="localhost";

		Map<String, String> inputParams = new HashMap<>();
		
		inputParams.put("capsuletype", "ristretto");
		inputParams.put("clientid", "myclientid");
		inputParams.put("port", "50051");
		inputParams.put("host", "localhost");		
		inputParams.put("functionality", "consumeCapsule");
		inputParams.put("waterTemp", "");
		inputParams.put("date", "");		
		inputParams.put("pressure", "");

        //Read input params
		for (String key : inputParams.keySet()) {
			for (int i = 0; i < args.length; i++){
				if(args[i].startsWith(key + ":")){
					inputParams.put(key, args[i].substring(key.length() + 1));
					break;
				}
			}
		}

        CoffeeServerClient client = new CoffeeServerClient(inputParams.get("host"), Integer.parseInt(inputParams.get("port")));
        try {
            if(inputParams.get("functionality").equals("consumeCapsule")){
                client.consumeCapsule(inputParams.get("clientid"), inputParams.get("capsuletype"));

            }else if(inputParams.get("functionality").equals("checkMachineStatus")){
                //Check if there are any missing parameters
                if(inputParams.get("waterTemp").length() == 0 || inputParams.get("date").length() == 0 || inputParams.get("pressure").length() == 0){
                    logger.info("Error: water temperature, date and pressure are mandatory parameters.");
                    System.exit(-1);
                }
            
                client.checkMachineStatus(Float.valueOf(inputParams.get("waterTemp")), Long.parseLong(inputParams.get("date")), Float.valueOf(inputParams.get("pressure")));
            }
        } finally {
            client.shutdown();
        }
    }
}

