package com.pi;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoapRequestManager {

    private static final Logger logger = LoggerFactory.getLogger(CoapRequestManager.class);

    public void sendACCommand(int floor, String onState, String setpoint) {
        String ip = FloorManager.getDeviceIP(floor, "actuator");
        if (ip == null) {
            logger.error("No actuator device found for floor {}", floor);
            System.err.println("No actuator device found for floor " + floor);
            return;
        }

        String url = String.format("coap://[%s]:5683/AC/setpoint?on=%s&setpoint=%s",
                ip, onState, setpoint);
        logger.info("Sending AC command to floor {}: on={}, setpoint={}", floor, onState, setpoint);
        sendRequest(url, "AC (Floor " + floor + ")");
    }

    public void sendWindowCommand(int floor, String setpoint) {
        String ip = FloorManager.getDeviceIP(floor, "actuator");
        if (ip == null) {
            logger.error("No actuator device found for floor {}", floor);
            System.err.println("No actuator device found for floor " + floor);
            return;
        }

        String url = String.format("coap://[%s]:5683/Window/setpoint?setpoint=%s", ip, setpoint);
        logger.info("Sending Window command to floor {}: setpoint={}", floor, setpoint);
        sendRequest(url, "Window (Floor " + floor + ")");
    }

    public void sendTemperatureCommand(int floor, String setpoint) {
        String ip = FloorManager.getDeviceIP(floor, "sensor");
        if (ip == null) {
            logger.error("No sensor device found for floor {}", floor);
            System.err.println("No sensor device found for floor " + floor);
            return;
        }

        String url = String.format("coap://[%s]:5683/TEMP/setpoint?setpoint=%s", ip, setpoint);
        logger.info("Sending Temperature command to floor {}: setpoint={}", floor, setpoint);
        sendRequest(url, "Temperature (Floor " + floor + ")");
    }

    public void sendLightCommand(int floor, String setpoint) {
        String ip = FloorManager.getDeviceIP(floor, "sensor");
        if (ip == null) {
            logger.error("No sensor device found for floor {}", floor);
            System.err.println("No sensor device found for floor " + floor);
            return;
        }

        String url = String.format("coap://[%s]:5683/LIGHT/setpoint?setpoint=%s", ip, setpoint);
        logger.info("Sending Light command to floor {}: setpoint={}", floor, setpoint);
        sendRequest(url, "Light (Floor " + floor + ")");
    }

    public void sendBatteryCommand(String setpoint) {
        String ip = FloorManager.getDeviceIP(0, "battery");
        if (ip == null) {
            logger.error("No battery device found for floor 0");
            System.err.println("No battery device found for floor 0");
            return;
        }

        String url = String.format("coap://[%s]:5683/battery/setpoint?setpoint=%s", ip, setpoint);
        logger.info("Sending Battery command to floor 0: setpoint={}", setpoint);
        sendRequest(url, "Battery (Floor 0)");
    }

    private void sendRequest(String url, String deviceType) {
        CoapClient client = new CoapClient(url);

        try {
            logger.debug("Sending {} command to: {}", deviceType, url);
            System.out.printf("Sending %s command...%n", deviceType);

            CoapResponse response = client.post("", MediaTypeRegistry.TEXT_PLAIN);

            if (response != null) {
                logger.info("{} Command Response - Code: {}, Text: {}, Success: {}",
                        deviceType, response.getCode(), response.getResponseText(), response.isSuccess());

                System.out.printf("=== %s Command Result ===%n", deviceType);
                System.out.println("Response Code: " + response.getCode());
                System.out.println("Response Text: " + response.getResponseText());
                System.out.println("Success: " + response.isSuccess());
            } else {
                logger.warn("No response received from {} device", deviceType);
                System.out.println("No response received from " + deviceType + " device.");
            }
        } catch (Exception e) {
            logger.error("Error sending {} command", deviceType, e);
            System.err.println("Error sending " + deviceType + " command: " + e.getMessage());
        } finally {
            client.shutdown();
            logger.debug("{} client shutdown", deviceType);
        }
    }

    public CoapResponse sendCustomRequest(String url, String method) {
        CoapClient client = new CoapClient(url);
        CoapResponse response = null;

        try {
            logger.info("Sending custom request: URL={}, Method={}", url, method);

            switch (method.toUpperCase()) {
                case "GET":
                    response = client.get();
                    break;
                case "POST":
                    response = client.post("", MediaTypeRegistry.TEXT_PLAIN);
                    break;
                case "PUT":
                    response = client.put("", MediaTypeRegistry.TEXT_PLAIN);
                    break;
                case "DELETE":
                    response = client.delete();
                    break;
                default:
                    logger.error("Unsupported method: {}", method);
                    System.err.println("Unsupported method: " + method);
                    return null;
            }

            if (response != null) {
                logger.info("Custom request response - Code: {}, Text: {}, Success: {}",
                        response.getCode(), response.getResponseText(), response.isSuccess());

                System.out.println("=== Custom Request Result ===");
                System.out.println("URL: " + url);
                System.out.println("Method: " + method);
                System.out.println("Response Code: " + response.getCode());
                System.out.println("Response Text: " + response.getResponseText());
                System.out.println("Success: " + response.isSuccess());
            }

        } catch (Exception e) {
            logger.error("Error sending custom request", e);
            System.err.println("Error sending custom request: " + e.getMessage());
        } finally {
            client.shutdown();
            logger.debug("Custom request client shutdown");
        }

        return response;
    }
}