package com.pi;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class MyServer extends CoapServer {

    public static void main(String[] args) {
        System.out.println("Running it!");
        //MyServer server = new MyServer();
        //server.add(new CoAPResourceExample("hellobella"));

        CoapClient client = new CoapClient("coap://[fd00::202:2:2:2]:5683/AC/setpoint?on=1&setpoint=18");

        // CoapResponse response = client.get();

        CoapResponse response = client.post("", MediaTypeRegistry.TEXT_PLAIN);

        if (response != null) {
            System.out.println("Response Code: " + response.getCode());
            System.out.println("Response Text: " + response.getResponseText());
        } else {
            System.out.println("No response received.");
        }
        client.shutdown();
        System.out.println("Client shutdown.");


         // Create CoAP client (point it to a resource that supports observation)
         CoapClient powerObs = new CoapClient("coap://[fd00::201:1:1:1]:5683/power");

        // Observe the resource
        powerObs.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                System.out.println("Notification Received:");
                System.out.println("Response Code: " + response.getCode());
                System.out.println("Payload: " + response.getResponseText());
            }

            @Override
            public void onError() {
                System.err.println("Observation failed or was canceled.");
            }
        });


        
         // Create CoAP client (point it to a resource that supports observation)
         CoapClient sensors = new CoapClient("coap://[fd00::202:2:2:2]:5683/SENSORS/reading");

        // Observe the resource
        sensors.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                System.out.println("Notification Received:");
                System.out.println("Response Code: " + response.getCode());
                System.out.println("Payload: " + response.getResponseText());
            }

            @Override
            public void onError() {
                System.err.println("Observation failed or was canceled.");
            }
        });


        // Keep the client running to receive updates
        try {
            Thread.sleep(60000); // Keep observing for 60 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        client.shutdown();
        System.out.println("Client shutdown.");
    


        //server.start();

    }
}
