package com.pi.BatteryControl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi.CoapRequestManager;
import com.pi.DatabaseManager;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BatteryControlService {

    private static final Logger logger = LoggerFactory.getLogger(BatteryControlService.class);

    private final BatteryControlAlgorithm controlAlgorithm;
    private final CoapRequestManager requestManager;
    private final DatabaseManager databaseManager;
    private final ScheduledExecutorService scheduler;

    private boolean isRunning = false;

    public BatteryControlService(CoapRequestManager requestManager, DatabaseManager databaseManager) {
        this.controlAlgorithm = new BatteryControlAlgorithm();
        this.requestManager = requestManager;
        this.databaseManager = databaseManager;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startAutomaticControl(int intervalSeconds) {
        if (isRunning) {
            logger.warn("Battery control is already running");
            return;
        }

        this.isRunning = true;

        scheduler.scheduleAtFixedRate(this::executeControlCycle, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        logger.info("Started automatic battery control with {}s interval", intervalSeconds);
    }

    public void executeControlCycle() {

        List<Double> activePowerHistory = databaseManager.getLastNPowerMeasurements(5, "active");
        List<Double> reactivePowerHistory = databaseManager.getLastNPowerMeasurements(5, "reactive");
        double currentSOC = databaseManager.getLastNBatteryMeasurements(1).get(0);
        double predictedPower = databaseManager.getLastPowerPrediction(); // Get the latest prediction

        double setpoint = controlAlgorithm.calculateSetpoint(activePowerHistory, reactivePowerHistory, currentSOC,
                predictedPower);

        sendBatterySetpoint(setpoint);

    }


    private void sendBatterySetpoint(double setpoint) {
        String setpointString = String.format(Locale.US, "%.2f", setpoint);
        requestManager.sendBatteryCommand(setpointString, false);
    }

    public void shutdown() {
        isRunning = false;
        scheduler.shutdown();
        logger.info("Stopped automatic battery control");

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}