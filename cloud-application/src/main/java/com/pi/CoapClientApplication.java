package com.pi;

import com.pi.BatteryControl.BatteryControlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class CoapClientApplication {

    private static final Logger logger = LoggerFactory.getLogger(CoapClientApplication.class);

    private DatabaseManager databaseManager;
    private CoapObserverManager observerManager;
    private CoapRequestManager requestManager;
    private UserInterface userInterface;
    private LogViewer logViewer;
    private BatteryControlService batteryControlService;

    public static void main(String[] args) {
        CoapClientApplication app = new CoapClientApplication();
        app.run();
    }

    public void run() {
        try {
            initialize();
            startApplication();
        } catch (Exception e) {
            logger.error("Application failed to start", e);
            System.err.println("Application failed to start: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    private void initialize() throws SQLException {
        logger.info("Starting CoAP Client Application initialization");

        logViewer = new LogViewer();
        userInterface = new UserInterface(null, null, logViewer); // Temporary
        userInterface.showStartupMessage();

        // Initialize database connection
        logger.info("Connecting to database...");
        databaseManager = new DatabaseManager();

        // Initialize CoAP components
        logger.info("Initializing CoAP components...");
        observerManager = new CoapObserverManager(databaseManager);
        requestManager = new CoapRequestManager();
        batteryControlService = new BatteryControlService(requestManager, databaseManager);

        // Update UI with proper references
        userInterface = new UserInterface(requestManager, databaseManager, logViewer);

        // Start observers
        logger.info("Starting observers...");
        observerManager.startAllObservers();

        logger.info("Starting automatic battery control...");
        batteryControlService.startAutomaticControl(30);

        logger.info("Initialization complete!");
    }



    private void startApplication() {
        logger.info("Starting main application loop");
        boolean running = true;

        while (running) {
            try {
                userInterface.displayMainMenu();
                int choice = userInterface.getChoice();

                logger.debug("User selected menu option: {}", choice);

                switch (choice) {
                    case 1:
                        userInterface.handleACRequest();
                        break;
                    case 2:
                        userInterface.handleWindowRequest();
                        break;
                    case 3:
                        userInterface.handleTemperatureRequest();
                        break;
                    case 4:
                        userInterface.handleLightRequest();
                        break;
                    case 5:
                        userInterface.handleBatteryRequest();
                        break;
                    case 6:
                        userInterface.handleCustomRequest();
                        break;
                    case 7:
                        userInterface.handleDataViewing();
                        break;
                    case 8:
                        userInterface.displaySystemInfo();
                        waitForEnter();
                        break;
                    case 9:
                        userInterface.handleLogViewing();
                        break;
                    case 10:
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }

                // Small delay to prevent overwhelming the console
                if (running && choice != 7 && choice != 9) {
                    Thread.sleep(500);
                }

            } catch (Exception e) {
                logger.error("Error during operation", e);
                System.err.println("Error during operation: " + e.getMessage());
            }
        }
    }

    private void waitForEnter() {
        System.out.print("\nPress Enter to continue...");
        try {
            System.in.read();
        } catch (Exception e) {
            // Ignore
        }
    }

    private void shutdown() {
        logger.info("Shutting down application");

        if (observerManager != null) {
            logger.info("Stopping observers");
            observerManager.stopAllObservers();
        }

        if (databaseManager != null) {
            logger.info("Closing database connection");
            databaseManager.close();
        }

        if (batteryControlService != null) {
            logger.info("Stopping battery control service");
            batteryControlService.shutdown();
        }

        if (userInterface != null) {
            userInterface.close();
            userInterface.showShutdownMessage();
        }

        logger.info("Application shutdown complete");
    }

    // Graceful shutdown hook
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LoggerFactory.getLogger(CoapClientApplication.class).info("Received shutdown signal");
        }));
    }
}