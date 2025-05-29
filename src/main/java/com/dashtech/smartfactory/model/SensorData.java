package com.dashtech.smartfactory.model;

import java.time.Instant;

/**
 * Record representing sensor data with immutable properties.
 * @param sensorId The unique identifier of the sensor
 * @param type The type of sensor (e.g., "Temperature", "Pressure")
 * @param value The measured value from the sensor
 * @param timestamp The time when the measurement was taken
 */
public record SensorData(
    int sensorId,
    String type,
    double value,
    Instant timestamp
) {
    /**
     * Factory method to create a new SensorData instance with current timestamp
     */
    public static SensorData create(int sensorId, String type, double value) {
        return new SensorData(sensorId, type, value, Instant.now());
    }
} 