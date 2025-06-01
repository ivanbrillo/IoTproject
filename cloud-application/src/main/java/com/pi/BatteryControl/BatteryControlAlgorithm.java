package com.pi.BatteryControl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BatteryControlAlgorithm {

    private static final Logger logger = LoggerFactory.getLogger(BatteryControlAlgorithm.class);

    private static final double MAX_SETPOINT = 10.0;
    private static final double MIN_SETPOINT = -10.0;
    private static final double PRICE_THRESHOLD_LOW = 12.0;
    private static final double PRICE_THRESHOLD_HIGH = 20.0;
    private static final double POWER_BALANCE_FACTOR = 0.001;

    private final ElectricityPriceProvider priceProvider;

    public BatteryControlAlgorithm() {
        this.priceProvider = new ElectricityPriceProvider();
    }

    public double calculateSetpoint(List<Double> historicalActivePower,
            List<Double> historicalReactivePower,
            double currentSOC,
            double predictedPower) {

        double currentPrice = priceProvider.getCurrentPrice();
        double currentAvgPower = historicalActivePower.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double priceBasedSetpoint = calculatePriceBasedSetpoint(currentPrice);
        double powerBalanceAdjustment = calculatePowerBalanceAdjustment(currentAvgPower, predictedPower);
        double socAdjustment = calculateSOCAdjustment(currentSOC);

        double setpoint = priceBasedSetpoint + powerBalanceAdjustment + socAdjustment;
        setpoint = Math.max(MIN_SETPOINT, Math.min(MAX_SETPOINT, setpoint));

        logger.info(
                "Battery setpoint: Price={:.2f}$/kWh, Current={:.2f}W, Predicted={:.2f}W, SOC={:.1f}%, Setpoint={:.2f}",
                currentPrice, currentAvgPower, predictedPower, currentSOC, setpoint);

        return setpoint;
    }

    private double calculatePriceBasedSetpoint(double price) {
        if (price < PRICE_THRESHOLD_LOW) {
            return 8.0; // Charge aggressively
        } else if (price < PRICE_THRESHOLD_HIGH) {
            double priceRange = PRICE_THRESHOLD_HIGH - PRICE_THRESHOLD_LOW;
            double pricePosition = (price - PRICE_THRESHOLD_LOW) / priceRange;
            return 4.0 * (1 - pricePosition);
        } else {
            return -6.0; // Discharge
        }
    }

    private double calculatePowerBalanceAdjustment(double currentPower, double predictedPower) {
        double powerDifference = predictedPower - currentPower;
        return -powerDifference * POWER_BALANCE_FACTOR;
    }

    private double calculateSOCAdjustment(double soc) {
        if (soc > 90)
            return -3.0;

        if (soc < 10)
            return 5.0;

        return 0.0;
    }

    public double getCurrentElectricityPrice() {
        return priceProvider.getCurrentPrice();
    }
}