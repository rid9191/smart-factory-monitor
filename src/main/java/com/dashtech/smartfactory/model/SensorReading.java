package com.dashtech.smartfactory.model;

import java.time.LocalDateTime;

public record SensorReading(int sensorId, String dataType, double value, LocalDateTime timestamp) {
    public SensorReading(int sensorId, String dataType, double value) {
        this(sensorId, dataType, value, LocalDateTime.now());
    }
}