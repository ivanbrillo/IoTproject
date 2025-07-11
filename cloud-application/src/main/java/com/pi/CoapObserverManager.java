package com.pi;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.Map;
import java.util.Set;

public class CoapObserverManager {

    private static final Logger logger = LoggerFactory.getLogger(CoapObserverManager.class);
    private static final long TIMEOUT_SECONDS = 60; // timeout interval

    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> timeoutTasks;
    private final Map<String, CoapObserveRelation> activeObservers;
    private final DatabaseManager databaseManager;
    private volatile boolean isShuttingDown = false;

    public CoapObserverManager(DatabaseManager databaseManager) {
        this.activeObservers = new ConcurrentHashMap<>();
        this.timeoutTasks = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        this.databaseManager = databaseManager;
        logger.info("CoapObserverManager initialized");
    }

    public void startAllObservers() {
        logger.info("Starting all observers");
        startBatteryObserver();
        startAllFloorSensorObservers();
        startPowerObserver();
        logger.info("All observers started. Active count: {}", activeObservers.size());
    }

    public void stopAllObservers() {
        logger.info("Stopping all observers");

        // cancel scheduled timeout tasks
        isShuttingDown = true;
        timeoutTasks.values().forEach(future -> future.cancel(true));
        timeoutTasks.clear();

        // cancel observe relations
        activeObservers.forEach((name, rel) -> {
            try {
                rel.proactiveCancel();
                logger.info("Cancelled observation: {}", name);
            } catch (Exception ignored) {
            }
        });

        // shutdown scheduler
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                logger.warn("Scheduler did not terminate in the allotted time");
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for scheduler to terminate");
            Thread.currentThread().interrupt();
        }

        activeObservers.clear();
        logger.info("All observers stopped");
    }

    private void scheduleTimeout(String observerName, Runnable timeoutAction) {
        if (isShuttingDown)
            return;

        // cancel existing
        ScheduledFuture<?> existing = timeoutTasks.get(observerName);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
        }
        // schedule new
        ScheduledFuture<?> future = scheduler.schedule(timeoutAction, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        timeoutTasks.put(observerName, future);
    }

    public boolean startSensorObserverForFloor(int floor) {
        String observerName = "sensors" + floor;
        String sensorIP = FloorManager.getDeviceIP(floor, "sensor");
        if (sensorIP == null) {
            logger.warn("No sensor IP for floor {}", floor);
            return false;
        }
        String url = "coap://[" + sensorIP + "]:5683/SENSORS/reading";
        logger.info("Starting {} at {}", observerName, url);

        CoapClient client = new CoapClient(url);
        CoapObserveRelation relation = client.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                String payload = response.getResponseText();
                logger.debug("{} notification: {}", observerName, payload);
                if (isValidPayload(payload, observerName)) {
                    databaseManager.storeSensorData(payload, floor);
                }
                // reset timeout on data
                scheduleTimeout(observerName,
                        () -> handleTimeout(observerName, () -> startSensorObserverForFloor(floor)));
            }

            @Override
            public void onError() {
                logger.error("{} error/canceled", observerName);
                cleanupObserver(observerName);
            }
        });

        activeObservers.put(observerName, relation);
        // schedule initial timeout
        scheduleTimeout(observerName, () -> handleTimeout(observerName, () -> startSensorObserverForFloor(floor)));

        return true;
    }

    private void startAllFloorSensorObservers() {
        Set<Integer> floors = FloorManager.getAvailableFloors();
        floors.forEach(this::startSensorObserverForFloor);
    }

    private void startBatteryObserver() {
        String observerName = "battery";
        String sensorIP = FloorManager.getDeviceIP(0, "battery");
        if (sensorIP == null) {
            logger.warn("No battery IP");
            return;
        }
        String url = "coap://[" + sensorIP + "]:5683/battery/soc";
        CoapClient client = new CoapClient(url);
        CoapObserveRelation relation = client.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                String payload = response.getResponseText();
                logger.debug("{} payload: {}", observerName, payload);
                if (isValidPayload(payload, observerName)) {
                    databaseManager.storeBatteryData(payload);
                }
                scheduleTimeout(observerName,
                        () -> handleTimeout(observerName, CoapObserverManager.this::startBatteryObserver));
            }

            @Override
            public void onError() {
                logger.error("{} error/canceled", observerName);
                cleanupObserver(observerName);
            }
        });
        activeObservers.put(observerName, relation);
        scheduleTimeout(observerName,
                () -> handleTimeout(observerName, CoapObserverManager.this::startBatteryObserver));
    }

    private void startPowerObserver() {
        String observerName = "power";
        String sensorIP = FloorManager.getDeviceIP(0, "battery");
        if (sensorIP == null) {
            logger.warn("No power IP");
            return;
        }
        String url = "coap://[" + sensorIP + "]:5683/power";
        CoapClient client = new CoapClient(url);
        CoapObserveRelation relation = client.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                String payload = response.getResponseText();
                logger.debug("{} payload: {}", observerName, payload);
                if (isValidPayload(payload, observerName)) {
                    databaseManager.storePowerData(payload);
                }
                scheduleTimeout(observerName,
                        () -> handleTimeout(observerName, CoapObserverManager.this::startPowerObserver));
            }

            @Override
            public void onError() {
                logger.error("{} error/canceled", observerName);
                cleanupObserver(observerName);
            }
        });
        activeObservers.put(observerName, relation);
        scheduleTimeout(observerName, () -> handleTimeout(observerName, CoapObserverManager.this::startPowerObserver));
    }

    private void handleTimeout(String observerName, Runnable restartAction) {
        logger.warn("{} timed out after {}s - restarting", observerName, TIMEOUT_SECONDS);
        cleanupObserver(observerName);
        databaseManager.resetVersionCounter(observerName);
        restartAction.run();
    }

    private void cleanupObserver(String observerName) {
        // cancel timeout
        ScheduledFuture<?> future = timeoutTasks.remove(observerName);
        if (future != null)
            future.cancel(true);
        // cancel relation
        CoapObserveRelation relation = activeObservers.remove(observerName);
        if (relation != null) {
            relation.proactiveCancel();
        }
    }

    public boolean stopSensorObserverForFloor(int floor) {
        String name = "sensors_floor_" + floor;
        return stopObserver(name);
    }

    public boolean stopObserver(String observerName) {
        if (!activeObservers.containsKey(observerName)) {
            logger.warn("Observer not found: {}", observerName);
            return false;
        }
        cleanupObserver(observerName);
        logger.info("Stopped observer: {}", observerName);
        return true;
    }

    private boolean isValidPayload(String payload, String observerType) {
        if (!payload.contains("\"v\"")) {
            logger.warn("Missing version for {}: {}", observerType, payload);
            return false;
        }
        return true;
    }
}
