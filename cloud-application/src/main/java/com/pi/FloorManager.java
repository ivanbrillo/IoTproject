package com.pi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FloorManager {
    
    private static final Map<Integer, Map<String, String>> floorDevices = new HashMap<>();
    
    static {
        // Floor 1 devices
        Map<String, String> floor1 = new HashMap<>();
        floor1.put("actuator", "fd00::203:3:3:3");  // AC and Window
        floor1.put("sensor", "fd00::202:2:2:2");     // Temperature and Light
        floor1.put("battery", "fd00::201:1:1:1");    // Battery
        floorDevices.put(1, floor1);
        
        // To add more floor:
        // Map<String, String> floor2 = new HashMap<>();
        // floor2.put("actuator", "[fd00::204:4:4:4]");
        // floor2.put("sensor", "[fd00::205:5:5:5]");
        // floor2.put("battery", "[fd00::206:6:6:6]");
        // floorDevices.put(2, floor2);
    }
    
    public static String getDeviceIP(int floor, String deviceType) {
        Map<String, String> devices = floorDevices.get(floor);
        if (devices != null) {
            return devices.get(deviceType);
        }
        return null;
    }
    
    public static Set<Integer> getAvailableFloors() {
        return floorDevices.keySet();
    }
    
    public static boolean isFloorValid(int floor) {
        return floorDevices.containsKey(floor);
    }
}