package com.dashtech.smartfactory.model;

import java.time.Instant;

/**
 * Record representing a command log entry with immutable properties.
 */
public record CommandLog(
    Long id,
    int actuatorId,
    String command,
    boolean success,
    String errorMessage,
    Instant timestamp,
    String operatorId
) {
    /**
     * Creates a successful command log entry
     */
    public static CommandLog success(ActuatorCommand cmd, String operatorId) {
        return new CommandLog(null, cmd.actuatorId(), cmd.command(), true, null, 
                            Instant.now(), operatorId);
    }
    
    /**
     * Creates a failed command log entry
     */
    public static CommandLog failure(ActuatorCommand cmd, String error, String operatorId) {
        return new CommandLog(null, cmd.actuatorId(), cmd.command(), false, error, 
                            Instant.now(), operatorId);
    }
} 