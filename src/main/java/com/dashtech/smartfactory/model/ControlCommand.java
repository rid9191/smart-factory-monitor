package com.dashtech.smartfactory.model;

import java.time.LocalDateTime;

public record ControlCommand(int actuatorId, String commandType, LocalDateTime timestamp) {
    public ControlCommand(int actuatorId, String commandType) {
        this(actuatorId, commandType, LocalDateTime.now());
    }
}