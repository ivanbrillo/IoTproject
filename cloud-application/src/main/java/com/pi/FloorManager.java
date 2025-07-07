package com.pi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FloorManager {

    private static final Map<Integer, Map<String, String>> floorDevices = new HashMap<>();

    static {
        // Floor 1 devices
        Map<String, String> floor1 = new HashMap<>();
        floor1.put("actuator", "fd00::203:3:3:3"); // AC and Window
        floor1.put("sensor", "fd00::f6ce:3651:2b3e:b419"); // Temperature and Light
        floorDevices.put(1, floor1);

        // Floor 0 devices
        Map<String, String> floor0 = new HashMap<>();
        floor0.put("battery", "fd00::f6ce:36a6:d989:7ca"); // Battery
        floor0.put("br", "fd00::f6ce:36a6:d989:7ca"); // Border Router
        floorDevices.put(0, floor0);
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