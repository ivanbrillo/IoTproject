package com.pi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String DB_URL = "jdbc:mysql://localhost:3306/sensors";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "iotubuntu";

    private Connection connection;
    private volatile boolean isShuttingDown = false;
    private double lastPredValue = 0.0; // Last prediction value for power

    // Version counters for each observable
    private Map<String, Integer> versionCounters = new ConcurrentHashMap<>();

    public DatabaseManager() throws SQLException {
        logger.info("Initializing database connection");
        this.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        logger.info("Database connection established successfully");

        // Initialize version counters
        versionCounters.put("power", -1);
        versionCounters.put("sensors", -1);
        versionCounters.put("battery", -1);
        logger.info("Version counters initialized: {}", versionCounters);
    }

    private boolean isConnectionValid() {
        if (isShuttingDown) {
            logger.debug("Database is shutting down, skipping operation");
            return false;
        }

        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            logger.warn("Error checking connection status", e);
            return false;
        }
    }

    private boolean shouldStoreData(String observableType, int newVersion) {
        int currentVersion = versionCounters.get(observableType);

        if (isVersionNewer(newVersion, currentVersion)) {
            versionCounters.put(observableType, newVersion);
            logger.debug("Version updated for {}: {} -> {}", observableType, currentVersion, newVersion);
            return true;
        } else {
            logger.debug("Skipping {} data - version {} not greater than current {}",
                    observableType, newVersion, currentVersion);
            return false;
        }
    }

    private boolean isVersionNewer(int newVersion, int currentVersion) {
        // Safe comparison that handles int overflow
        return (newVersion - currentVersion) > 0;
    }

    public void storePowerData(String jsonPayload) {
        if (!isConnectionValid()) {
            logger.debug("Skipping power data storage - connection not available");
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(jsonPayload).getAsJsonObject();

            // Get version from JSON
            int version = json.get("v").getAsInt();

            // Check if we should store this version
            if (!shouldStoreData("power", version)) {
                return;
            }

            lastPredValue = json.get("pred").getAsDouble();

            // Handle the "last" array - using last[0] as active, last[1] as reactive
            JsonArray lastArray = json.getAsJsonArray("last");
            double activeValue = lastArray.get(0).getAsDouble();
            double reactiveValue = lastArray.get(1).getAsDouble();

            String sql = "INSERT INTO power (value_active, value_reactive) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setDouble(1, activeValue);
                pstmt.setDouble(2, reactiveValue);
                pstmt.executeUpdate();
                logger.info("Power data stored (v{}): Active={}, Reactive={}",
                        version, activeValue, reactiveValue);
            }
        } catch (SQLException e) {
            if (!isShuttingDown) {
                logger.error("Error storing power data: {}", jsonPayload, e);
            }
        } catch (Exception e) {
            logger.error("Error storing power data: {}", jsonPayload, e);
        }
    }

    public void storeSensorData(String jsonPayload, int floor) {
        if (!isConnectionValid()) {
            logger.debug("Skipping sensor data storage - connection not available");
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(jsonPayload).getAsJsonObject();

            // Get version from JSON
            int version = json.get("v").getAsInt();

            // Check if we should store this version
            if (!shouldStoreData("sensors", version)) {
                return;
            }

            double lightValue = json.get("light").getAsDouble();
            double tempValue = json.get("temp").getAsDouble();

            // Store light data
            String lightSql = "INSERT INTO light (value, floor) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(lightSql)) {
                pstmt.setDouble(1, lightValue);
                pstmt.setInt(2, floor);
                pstmt.executeUpdate();
            }

            // Store temperature data
            String tempSql = "INSERT INTO temperature (value, floor) VALUES (?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(tempSql)) {
                pstmt.setDouble(1, tempValue);
                pstmt.setInt(2, floor);
                pstmt.executeUpdate();
            }

            logger.info("Sensor data stored (v{}): Light={}, Temp={} (Floor 1)",
                    version, lightValue, tempValue);
        } catch (SQLException e) {
            if (!isShuttingDown) {
                logger.error("Error storing sensor data: {}", jsonPayload, e);
            }
        } catch (Exception e) {
            logger.error("Error storing sensor data: {}", jsonPayload, e);
        }
    }

    public void storeBatteryData(String jsonPayload) {
        if (!isConnectionValid()) {
            logger.debug("Skipping battery data storage - connection not available");
            return;
        }

        try {
            JsonObject json = JsonParser.parseString(jsonPayload).getAsJsonObject();

            // Get version from JSON
            int version = json.get("v").getAsInt();

            // Check if we should store this version
            if (!shouldStoreData("battery", version)) {
                return;
            }

            double socValue = json.get("soc").getAsDouble();

            // Store battery data
            String sql = "INSERT INTO battery (value) VALUES (?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setDouble(1, socValue);
                pstmt.executeUpdate();
                logger.info("Battery data stored (v{}): SOC={}", version, socValue);
            }
        } catch (SQLException e) {
            if (!isShuttingDown) {
                logger.error("Error storing battery data: {}", jsonPayload, e);
            }
        } catch (Exception e) {
            logger.error("Error storing battery data: {}", jsonPayload, e);
        }
    }

    public void viewBatteryData() {
        if (!isConnectionValid()) {
            System.err.println("Database connection not available");
            return;
        }

        logger.info("Viewing battery data");
        String sql = "SELECT * FROM battery ORDER BY timestamp DESC LIMIT 10";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n=== Recent Battery Data ===");
            System.out.printf("%-5s %-20s %-10s%n", "ID", "Timestamp", "SOC (%)");
            System.out.println("---------------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d %-20s %-10.3f%n",
                        rs.getInt("id"),
                        rs.getTimestamp("timestamp").toString(),
                        rs.getFloat("value"));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving battery data", e);
            System.err.println("Error retrieving battery data: " + e.getMessage());
        }
    }

    public void viewPowerData() {
        if (!isConnectionValid()) {
            System.err.println("Database connection not available");
            return;
        }

        logger.info("Viewing power data");
        String sql = "SELECT * FROM power ORDER BY timestamp DESC LIMIT 10";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n=== Recent Power Data ===");
            System.out.printf("%-5s %-20s %-12s %-12s%n",
                    "ID", "Timestamp", "Active", "Reactive");
            System.out.println("-------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d %-20s %-12.3f %-12.3f%n",
                        rs.getInt("id"),
                        rs.getTimestamp("timestamp").toString(),
                        rs.getFloat("value_active"),
                        rs.getFloat("value_reactive"));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving power data", e);
            System.err.println("Error retrieving power data: " + e.getMessage());
        }
    }

    public void viewTemperatureData() {
        if (!isConnectionValid()) {
            System.err.println("Database connection not available");
            return;
        }

        logger.info("Viewing temperature data");
        String sql = "SELECT * FROM temperature ORDER BY timestamp DESC LIMIT 10";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n=== Recent Temperature Data ===");
            System.out.printf("%-5s %-20s %-10s %-5s%n", "ID", "Timestamp", "Value", "Floor");
            System.out.println("-----------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d %-20s %-10.3f %-5d%n",
                        rs.getInt("id"),
                        rs.getTimestamp("timestamp").toString(),
                        rs.getFloat("value"),
                        rs.getInt("floor"));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving temperature data", e);
            System.err.println("Error retrieving temperature data: " + e.getMessage());
        }
    }

    public void viewLightData() {
        if (!isConnectionValid()) {
            System.err.println("Database connection not available");
            return;
        }

        logger.info("Viewing light data");
        String sql = "SELECT * FROM light ORDER BY timestamp DESC LIMIT 10";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n=== Recent Light Data ===");
            System.out.printf("%-5s %-20s %-10s %-5s%n", "ID", "Timestamp", "Value", "Floor");
            System.out.println("-----------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d %-20s %-10.3f %-5d%n",
                        rs.getInt("id"),
                        rs.getTimestamp("timestamp").toString(),
                        rs.getFloat("value"),
                        rs.getInt("floor"));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving light data", e);
            System.err.println("Error retrieving light data: " + e.getMessage());
        }
    }

    // Method to get current version counters (for debugging/monitoring)
    public Map<String, Integer> getVersionCounters() {
        return new ConcurrentHashMap<>(versionCounters);
    }

    // Method to reset version counters (if needed for testing)
    public void resetVersionCounters() {
        versionCounters.replaceAll((k, v) -> 0);
        logger.info("Version counters reset to 0");
    }

    public void close() {
        logger.info("Starting database shutdown");
        isShuttingDown = true;

        logger.info("Final version counters: {}", versionCounters);

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed");
            }
        } catch (SQLException e) {
            logger.error("Error closing database connection", e);
        }
    }

    public List<Double> getLastNPowerMeasurements(int n, String powerType) {
        List<Double> measurements = new ArrayList<>();
        if (!isConnectionValid())
            return measurements;

        String column = powerType.equals("active") ? "value_active" : "value_reactive";
        String sql = "SELECT " + column + " FROM power ORDER BY timestamp DESC LIMIT ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, n);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    measurements.add(rs.getDouble(1));
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving power measurements", e);
        }

        Collections.reverse(measurements);
        return measurements;
    }

    public List<Double> getLastNBatteryMeasurements(int n) {
        List<Double> measurements = new ArrayList<>();
        if (!isConnectionValid())
            return measurements;

        String sql = "SELECT value FROM battery ORDER BY timestamp DESC LIMIT ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, n);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    measurements.add(rs.getDouble("value"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving battery measurements", e);
        }

        Collections.reverse(measurements);
        return measurements;
    }

    public double getLastPowerPrediction() {
        return lastPredValue;
    }

}