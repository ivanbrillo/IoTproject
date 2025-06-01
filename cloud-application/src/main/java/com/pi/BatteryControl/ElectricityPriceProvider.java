package com.pi.BatteryControl;

import java.util.Random;

public class ElectricityPriceProvider {

    private static final double BASE_PRICE = 15.0;
    private static final double PEAK_MULTIPLIER = 1.8;
    private static final double OFF_PEAK_MULTIPLIER = 0.6;
    private static final double RANDOM_VARIATION = 0.2;
    private static int hour = 0;
    private final Random random;

    public ElectricityPriceProvider() {
        this.random = new Random();
    }

    public double getCurrentPrice() {
        double price = calculateTimeBasedPrice((hour++) % 25);

        double variation = (random.nextDouble() - 0.5) * 2 * RANDOM_VARIATION;
        price = price * (1 + variation);

        return Math.max(price, BASE_PRICE * 0.1);
    }

    private double calculateTimeBasedPrice(int hour) {
        if ((hour >= 8 && hour <= 10) || (hour >= 18 && hour <= 21)) {
            return BASE_PRICE * PEAK_MULTIPLIER;
        } else if (hour >= 23 || hour <= 6) {
            return BASE_PRICE * OFF_PEAK_MULTIPLIER;
        } else {
            return BASE_PRICE;
        }
    }
}