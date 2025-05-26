package com.pi;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;

public class CoapObserverManager {
    
    private static final Logger logger = LoggerFactory.getLogger(CoapObserverManager.class);
    
    private Map<String, CoapClient> activeClients;
    private Map<String, CoapObserveRelation> activeObservers;
    private DatabaseManager databaseManager;
    
    public CoapObserverManager(DatabaseManager databaseManager) {
        this.activeClients = new ConcurrentHashMap<>();
        this.activeObservers = new ConcurrentHashMap<>();
        this.databaseManager = databaseManager;
        logger.info("CoapObserverManager initialized");
    }
    
    public void startAllObservers() {
        logger.info("Starting all observers");
        startBatteryObserver();
        startAllFloorSensorObservers(); // Updated to start all floors
        startPowerObserver();
        logger.info("All observers started successfully. Active count: {}", activeObservers.size());
    }
    
    // New method to start sensor observers for all floors
    public void startAllFloorSensorObservers() {
        logger.info("Starting sensor observers for all floors");
        Set<Integer> availableFloors = FloorManager.getAvailableFloors();
        
        for (Integer floor : availableFloors) {
            startSensorObserverForFloor(floor);
        }
        
        logger.info("Started sensor observers for {} floors", availableFloors.size());
    }
    
    // Method to start sensor observer for a specific floor (public interface)
    public boolean startSensorObserverForFloor(int floor) {
        if (!FloorManager.isFloorValid(floor)) {
            logger.error("Invalid floor number: {}", floor);
            return false;
        }
        
        String observerName = "sensors_floor_" + floor;
        if (activeObservers.containsKey(observerName)) {
            logger.warn("Sensor observer for floor {} already exists", floor);
            return false;
        }
        
        String sensorIP = FloorManager.getDeviceIP(floor, "sensor");
        if (sensorIP == null) {
            logger.warn("No sensor IP found for floor {}", floor);
            return false;
        }
        
        String coapUrl = "coap://[" + sensorIP + "]:5683/SENSORS/reading";
        
        logger.info("Starting sensor observer for floor {} at {}", floor, coapUrl);
        
        CoapClient sensorsClient = new CoapClient(coapUrl);
        CoapObserveRelation sensorsRelation = sensorsClient.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                String payload = response.getResponseText();
                logger.debug("Floor {} sensors notification received - Code: {}, Payload: {}", 
                           floor, response.getCode(), payload);
                
                if (isValidPayload(payload, observerName)) {
                    // Store sensor data with floor information
                    databaseManager.storeSensorData(payload, floor);
                }
            }

            @Override
            public void onError() {
                logger.error("Floor {} sensors observation failed or was canceled", floor);
                activeObservers.remove(observerName);
                activeClients.remove(observerName);
            }
        });
        
        activeClients.put(observerName, sensorsClient);
        activeObservers.put(observerName, sensorsRelation);
        logger.info("Sensor observer started for floor {}", floor);
        return true;
    }
    
    // Method to stop sensor observer for a specific floor
    public boolean stopSensorObserverForFloor(int floor) {
        String observerName = "sensors_floor_" + floor;
        return stopObserver(observerName);
    }
    
    private boolean isValidPayload(String payload, String observerType) {
        if (payload == null || payload.trim().isEmpty()) {
            logger.warn("Empty payload received for {}", observerType);
            return false;
        }
        
        String trimmedPayload = payload.trim();
        
        // Check for known error messages
        if ("TooManyObservers".equals(trimmedPayload) || 
            "NotObservable".equals(trimmedPayload) || 
            "Error".equals(trimmedPayload)) {
            logger.warn("Error message received for {}: {}", observerType, trimmedPayload);
            return false;
        }
        
        // Check if it contains version field
        if (!trimmedPayload.contains("\"v\"")) {
            logger.warn("Missing version field for {} - payload: {}", observerType, payload);
            return false;
        }
        
        return true;
    }
    
    // Stop a specific observer by name
    public boolean stopObserver(String observerName) {
        CoapObserveRelation relation = activeObservers.get(observerName);
        CoapClient client = activeClients.get(observerName);
        
        if (relation != null && client != null) {
            try {
                // Cancel the observe relationship
                relation.proactiveCancel();
                // Shutdown the client
                client.shutdown();
                
                activeObservers.remove(observerName);
                activeClients.remove(observerName);
                
                logger.info("Stopped observer: {}", observerName);
                return true;
            } catch (Exception e) {
                logger.error("Error stopping observer {}", observerName, e);
                return false;
            }
        }
        logger.warn("Observer not found: {}", observerName);
        return false;
    }
    

    
    private void startBatteryObserver() {
        logger.info("Starting battery SOC observer");
        CoapClient socClient = new CoapClient("coap://[fd00::201:1:1:1]:5683/battery/soc");
        CoapObserveRelation socRelation = socClient.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                String payload = response.getResponseText();
                logger.debug("Battery SOC notification received - Code: {}, Payload: {}", 
                           response.getCode(), payload);
                
                if (isValidPayload(payload, "battery_soc")) {
                    databaseManager.storeBatteryData(payload);
                }
            }

            @Override
            public void onError() {
                logger.error("Battery SOC observation failed or was canceled");
                activeObservers.remove("battery_soc");
                activeClients.remove("battery_soc");
            }
        });
        
        activeClients.put("battery_soc", socClient);
        activeObservers.put("battery_soc", socRelation);
        logger.info("Battery SOC observer started");
    }

    
    private void startPowerObserver() {
        logger.info("Starting power observer");
        CoapClient powerClient = new CoapClient("coap://[fd00::201:1:1:1]:5683/power");
        CoapObserveRelation powerRelation = powerClient.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                String payload = response.getResponseText();
                logger.debug("Power notification received - Code: {}, Payload: {}", 
                           response.getCode(), payload);
                
                if (isValidPayload(payload, "power")) {
                    databaseManager.storePowerData(payload);
                }
            }

            @Override
            public void onError() {
                logger.error("Power observation failed or was canceled");
                activeObservers.remove("power");
                activeClients.remove("power");
            }
        });
        
        activeClients.put("power", powerClient);
        activeObservers.put("power", powerRelation);
        logger.info("Power observer started");
    }
    
    public void stopAllObservers() {
        logger.info("Stopping all observers");
        
        for (Map.Entry<String, CoapObserveRelation> entry : activeObservers.entrySet()) {
            try {
                entry.getValue().proactiveCancel();
                logger.info("Cancelled observation: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Error cancelling observation {}", entry.getKey(), e);
            }
        }
        
        for (Map.Entry<String, CoapClient> entry : activeClients.entrySet()) {
            try {
                entry.getValue().shutdown();
                logger.info("Shutdown client: {}", entry.getKey());
            } catch (Exception e) {
                logger.error("Error shutting down client {}", entry.getKey(), e);
            }
        }
        
        activeObservers.clear();
        activeClients.clear();
        logger.info("All observers stopped");
    }
    
    public void restartAllObservers() {
        logger.info("Restarting all observers");
        stopAllObservers();
        startAllObservers();
        logger.info("All observers restarted");
    }
    
    public int getActiveObserverCount() {
        return activeObservers.size();
    }
    
    public boolean isObserverActive(String observerName) {
        return activeObservers.containsKey(observerName);
    }
    
    // Get the observe relation for advanced operations
    public CoapObserveRelation getObserveRelation(String observerName) {
        return activeObservers.get(observerName);
    }
}