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

    public void sendDynamicControlCommand(int floor, String onState) {
        String ip = FloorManager.getDeviceIP(floor, "sensor");
        if (ip == null) {
            logger.error("No sensor device found for floor {}", floor);
            System.err.println("No sensor device found for floor " + floor);
            return;
        }

        // Validate onState parameter
        if (!onState.equals("0") && !onState.equals("1")) {
            logger.error("Invalid dynamic control state: {}. Must be 0 or 1", onState);
            System.err.println("Invalid state: " + onState + ". Must be 0 or 1");
            return;
        }

        String url = String.format("coap://[%s]:5683/dynamic-control?on=%s", ip, onState);
        logger.info("Sending Dynamic Control command to floor {}: on={}", floor, onState);
        sendRequest(url, "Dynamic Control (Floor " + floor + ")");
    }

    public void sendBatteryCommand(String setpoint, boolean verbose) {
        String ip = FloorManager.getDeviceIP(0, "battery");
        if (ip == null) {
            logger.error("No battery device found for floor 0");
            System.err.println("No battery device found for floor 0");
            return;
        }

        String url = String.format("coap://[%s]:5683/battery/setpoint?setpoint=%s", ip, setpoint);
        logger.info("Sending Battery command to floor 0: setpoint={}", setpoint);
        sendRequest(url, "Battery (Floor 0)", verbose);
    }

    private void sendRequest(String url, String deviceType) {
        sendRequest(url, deviceType, true);
    }

    private void sendRequest(String url, String deviceType, boolean verbose) {
        CoapClient client = new CoapClient(url);

        try {
            logger.debug("Sending {} command to: {}", deviceType, url);
            if (verbose) {
                System.out.printf("Sending %s command...%n", deviceType);
            }

            CoapResponse response = client.post("", MediaTypeRegistry.TEXT_PLAIN);

            if (response != null) {
                logger.info("{} Command Response - Code: {}, Text: {}, Success: {}",
                        deviceType, response.getCode(), response.getResponseText(), response.isSuccess());

                if (verbose) {
                    System.out.printf("=== %s Command Result ===%n", deviceType);
                    System.out.println("Response Code: " + response.getCode());
                    System.out.println("Response Text: " + response.getResponseText());
                    System.out.println("Success: " + response.isSuccess());
                }
            } else {
                logger.warn("No response received from {} device", deviceType);
                if (verbose)
                    System.out.println("No response received from " + deviceType + " device.");

            }
        } catch (Exception e) {
            logger.error("Error sending {} command", deviceType, e);
            if (verbose)
                System.err.println("Error sending " + deviceType + " command: " + e.getMessage());

        } finally {
            client.shutdown();
            logger.debug("{} client shutdown", deviceType);
        }
    }

}
