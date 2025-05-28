package com.pi;

import com.pi.CoapRequestManager;
import com.pi.DatabaseManager;
import com.pi.LogViewer;
import com.pi.FloorManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

public class UserInterface {

    private static final Logger logger = LoggerFactory.getLogger(UserInterface.class);

    private Scanner scanner;
    private CoapRequestManager requestManager;
    private DatabaseManager databaseManager;
    private LogViewer logViewer;

    public UserInterface(CoapRequestManager requestManager, DatabaseManager databaseManager, LogViewer logViewer) {
        this.scanner = new Scanner(System.in);
        this.requestManager = requestManager;
        this.databaseManager = databaseManager;
        this.logViewer = logViewer;
        logger.info("UserInterface initialized");
    }

    public void displayMainMenu() {
        System.out.println("\n=== CoAP Client Manager ===");
        System.out.println("Available floors: " + FloorManager.getAvailableFloors());
        System.out.println("1. Send AC Command");
        System.out.println("2. Send Window Command");
        System.out.println("3. Send Temperature Command");
        System.out.println("4. Send Light Command");
        System.out.println("5. Send Battery Command");
        System.out.println("6. Send Custom Request");
        System.out.println("7. Send Dynamic Control Command");  
        System.out.println("8. View Stored Data");
        System.out.println("9. System Information");
        System.out.println("10. View Logs");
        System.out.println("11. Exit");
        System.out.print("Choose an option (1-11): ");
    }

    private int getFloor() {
        System.out.print("Enter floor number: ");
        try {
            int floor = Integer.parseInt(scanner.nextLine().trim());
            if (FloorManager.isFloorValid(floor)) {
                return floor;
            } else {
                System.out.println("Invalid floor. Available floors: " + FloorManager.getAvailableFloors());
                return -1;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid floor number.");
            return -1;
        }
    }

    public int getChoice() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public void handleACRequest() {
        int floor = getFloor();
        if (floor == -1)
            return;

        System.out.print("Enter AC state (0=off, 1=on): ");
        String onState = scanner.nextLine().trim();

        if (!isValidInput(onState)) {
            System.out.println("Invalid input. Please try again.");
            logger.warn("Invalid AC command input received");
            return;
        }

        String setpoint = "20.0";

        if (onState.equals("1")) {
            System.out.print("Enter setpoint temperature: ");
            setpoint = scanner.nextLine().trim();

            if (!isValidInput(setpoint)) {
                System.out.println("Invalid input. Please try again.");
                logger.warn("Invalid AC command input received");
                return;
            }
        }

        logger.info("User requested AC command for floor {}: on={}, setpoint={}", floor, onState, setpoint);
        requestManager.sendACCommand(floor, onState, setpoint);
    }

    public void handleWindowRequest() {
        int floor = getFloor();
        if (floor == -1)
            return;

        System.out.print("Enter window setpoint: ");
        String setpoint = scanner.nextLine().trim();

        if (isValidInput(setpoint)) {
            logger.info("User requested Window command for floor {}: setpoint={}", floor, setpoint);
            requestManager.sendWindowCommand(floor, setpoint);
        } else {
            System.out.println("Invalid input. Please try again.");
            logger.warn("Invalid Window command input received");
        }
    }

    public void handleTemperatureRequest() {
        int floor = getFloor();
        if (floor == -1)
            return;

        System.out.print("Enter temperature setpoint: ");
        String setpoint = scanner.nextLine().trim();

        if (isValidInput(setpoint)) {
            logger.info("User requested Temperature command for floor {}: setpoint={}", floor, setpoint);
            requestManager.sendTemperatureCommand(floor, setpoint);
        } else {
            System.out.println("Invalid input. Please try again.");
            logger.warn("Invalid Temperature command input received");
        }
    }

    public void handleLightRequest() {
        int floor = getFloor();
        if (floor == -1)
            return;

        System.out.print("Enter light setpoint: ");
        String setpoint = scanner.nextLine().trim();

        if (isValidInput(setpoint)) {
            logger.info("User requested Light command for floor {}: setpoint={}", floor, setpoint);
            requestManager.sendLightCommand(floor, setpoint);
        } else {
            System.out.println("Invalid input. Please try again.");
            logger.warn("Invalid Light command input received");
        }
    }

    public void handleBatteryRequest() {

        System.out.print("Enter battery setpoint: ");
        String setpoint = scanner.nextLine().trim();

        if (isValidInput(setpoint)) {
            logger.info("User requested Battery command for floor 0: setpoint={}", setpoint);
            requestManager.sendBatteryCommand(setpoint, true);
        } else {
            System.out.println("Invalid input. Please try again.");
            logger.warn("Invalid Battery command input received");
        }
    }

    public void handleDynamicControlRequest() {
        int floor = getFloor();
        if (floor == -1)
            return;
    
        System.out.print("Enter dynamic control state (0=disable, 1=enable): ");
        String onState = scanner.nextLine().trim();
    
        if (onState.equals("0") || onState.equals("1")) {
            logger.info("User requested Dynamic Control command for floor {}: on={}", floor, onState);
            requestManager.sendDynamicControlCommand(floor, onState);
        } else {
            System.out.println("Invalid input. Please enter 0 or 1.");
            logger.warn("Invalid Dynamic Control command input received: {}", onState);
        }
    }

    // ...existing code for other methods...
    public void handleCustomRequest() {
        System.out.print("Enter CoAP URL: ");
        String url = scanner.nextLine().trim();
        System.out.print("Enter HTTP method (GET/POST/PUT/DELETE): ");
        String method = scanner.nextLine().trim();

        if (isValidInput(url) && isValidInput(method)) {
            logger.info("User requested Custom command: url={}, method={}", url, method);
            requestManager.sendCustomRequest(url, method);
        } else {
            System.out.println("Invalid input. Please try again.");
            logger.warn("Invalid Custom command input received");
        }
    }

    public void displayDataMenu() {
        System.out.println("\n=== Data Viewer ===");
        System.out.println("1. View Power Data");
        System.out.println("2. View Temperature Data");
        System.out.println("3. View Light Data");
        System.out.println("4. View Battery SOC Data");
        System.out.println("5. Back to Main Menu");
        System.out.print("Choose data type (1-5): ");
    }

    public void handleDataViewing() {
        boolean inDataMenu = true;
        while (inDataMenu) {
            displayDataMenu();
            int choice = getChoice();

            switch (choice) {
                case 1:
                    logger.info("User viewing power data");
                    databaseManager.viewPowerData();
                    break;
                case 2:
                    logger.info("User viewing temperature data");
                    databaseManager.viewTemperatureData();
                    break;
                case 3:
                    logger.info("User viewing light data");
                    databaseManager.viewLightData();
                    break;
                case 4:
                    logger.info("User viewing battery SOC data");
                    databaseManager.viewBatteryData();
                    break;
                case 5:
                    inDataMenu = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }

            if (inDataMenu) {
                System.out.print("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    public void displayLogMenu() {
        System.out.println("\n=== Log Viewer ===");
        System.out.println("1. View Recent Logs");
        System.out.println("2. View Last N Lines");
        System.out.println("3. Clear Logs");
        System.out.println("4. Back to Main Menu");
        System.out.print("Choose option (1-4): ");
    }

    public void handleLogViewing() {
        boolean inLogMenu = true;
        while (inLogMenu) {
            displayLogMenu();
            int choice = getChoice();

            switch (choice) {
                case 1:
                    logViewer.showRecentLogs();
                    break;
                case 2:
                    System.out.print("Enter number of lines to show: ");
                    try {
                        int n = Integer.parseInt(scanner.nextLine().trim());
                        logViewer.showLastNLines(n);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number format.");
                    }
                    break;
                case 3:
                    System.out.print("Are you sure you want to clear all logs? (y/N): ");
                    String confirm = scanner.nextLine().trim();
                    if ("y".equalsIgnoreCase(confirm) || "yes".equalsIgnoreCase(confirm)) {
                        logViewer.clearLogs();
                    }
                    break;
                case 4:
                    inLogMenu = false;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }

            if (inLogMenu) {
                System.out.print("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
    }

    public void displaySystemInfo() {
        System.out.println("\n=== System Information ===");
        System.out.println("CoAP Client Manager v1.0");
        System.out.println("Database: sensors");
        System.out.println("Available tables: power, temperature, light");
        System.out.println("Available floors: " + FloorManager.getAvailableFloors());
        System.out.println("Active observers: 3 (battery, sensors, power)");
        System.out.println("Status: Running");

        // Runtime information
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        System.out.printf("Memory Usage: %.2f MB / %.2f MB%n",
                usedMemory / 1024.0 / 1024.0,
                totalMemory / 1024.0 / 1024.0);
    }

    public void showStartupMessage() {
        System.out.println("====================================");
        System.out.println("    CoAP Client Manager Started    ");
        System.out.println("====================================");
        System.out.println("Initializing components...");
    }

    public void showShutdownMessage() {
        System.out.println("\n====================================");
        System.out.println("   CoAP Client Manager Shutdown    ");
        System.out.println("====================================");
        System.out.println("Thank you for using the system!");
    }

    private boolean isValidInput(String input) {
        return input != null && !input.trim().isEmpty();
    }

    public void close() {
        if (scanner != null) {
            scanner.close();
        }
        logger.info("UserInterface closed");
    }
}