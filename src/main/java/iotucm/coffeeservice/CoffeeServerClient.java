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
            client.consumeCapsule(inputParams.get("clientid"), inputParams.get("capsuletype"));
        } finally {
            client.shutdown();
        }
    }
}

