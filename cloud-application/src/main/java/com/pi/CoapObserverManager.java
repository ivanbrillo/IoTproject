package com.pi;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.pi.DatabaseManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

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
        startSensorsObserver();
        startPowerObserver();
        logger.info("All observers started successfully. Active count: {}", activeObservers.size());
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
    
    // Restart a specific observer
    public boolean restartObserver(String observerName) {
        if (stopObserver(observerName)) {
            switch (observerName) {
                case "battery_soc":
                    startBatteryObserver();
                    break;
                case "sensors":
                    startSensorsObserver();
                    break;
                case "power":
                    startPowerObserver();
                    break;
                default:
                    logger.error("Unknown observer name: {}", observerName);
                    return false;
            }
            logger.info("Restarted observer: {}", observerName);
            return true;
        }
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
    
    private void startSensorsObserver() {
        logger.info("Starting sensors observer");
        CoapClient sensorsClient = new CoapClient("coap://[fd00::202:2:2:2]:5683/SENSORS/reading");
        CoapObserveRelation sensorsRelation = sensorsClient.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                String payload = response.getResponseText();
                logger.debug("Sensors notification received - Code: {}, Payload: {}", 
                           response.getCode(), payload);
                
                if (isValidPayload(payload, "sensors")) {
                    databaseManager.storeSensorData(payload);
                }
            }

            @Override
            public void onError() {
                logger.error("Sensors observation failed or was canceled");
                activeObservers.remove("sensors");
                activeClients.remove("sensors");
            }
        });
        
        activeClients.put("sensors", sensorsClient);
        activeObservers.put("sensors", sensorsRelation);
        logger.info("Sensors observer started");
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