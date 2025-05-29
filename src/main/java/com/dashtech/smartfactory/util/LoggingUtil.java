package com.dashtech.smartfactory.util;

import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 * Utility class for standardized logging across the application.
 */
public final class LoggingUtil {
    private LoggingUtil() {} // Prevent instantiation
    
    /**
     * Log application startup with component details
     */
    public static void logStartup(Logger logger, String componentName, Map<String, String> details) {
        ThreadContext.put("component", componentName);
        logger.info("Starting {} with configuration: {}", componentName, details);
        ThreadContext.remove("component");
    }
    
    /**
     * Log application shutdown with component details
     */
    public static void logShutdown(Logger logger, String componentName, Map<String, String> details) {
        ThreadContext.put("component", componentName);
        logger.info("Shutting down {} - Status: {}", componentName, details);
        ThreadContext.remove("component");
    }
    
    /**
     * Log serial communication events
     */
    public static void logSerialEvent(Logger logger, String event, String portName, String details) {
        ThreadContext.put("port", portName);
        logger.debug("Serial Event [{}] on port {}: {}", event, portName, details);
        ThreadContext.remove("port");
    }
    
    /**
     * Log WebSocket events with session details
     */
    public static void logWebSocketEvent(Logger logger, String event, String sessionId, String details) {
        ThreadContext.put("session", sessionId);
        logger.debug("WebSocket Event [{}] for session {}: {}", event, sessionId, details);
        ThreadContext.remove("session");
    }
    
    /**
     * Log database operations with transaction context
     */
    public static void logDatabaseOperation(Logger logger, String operation, String details) {
        logger.debug("Database Operation [{}]: {}", operation, details);
    }
    
    /**
     * Log transaction events
     */
    public static void logTransaction(Logger logger, String transactionId, String event, String details) {
        ThreadContext.put("transaction", transactionId);
        logger.debug("Transaction [{}] {}: {}", transactionId, event, details);
        ThreadContext.remove("transaction");
    }
    
    /**
     * Log errors with full context and optional stack trace
     */
    public static void logError(Logger logger, String context, String message, Throwable error) {
        ThreadContext.put("errorContext", context);
        logger.error("Error in {}: {} - {}", context, message, error.getMessage(), error);
        ThreadContext.remove("errorContext");
    }
    
    /**
     * Log sensor data events
     */
    public static void logSensorData(Logger logger, int sensorId, String type, double value) {
        ThreadContext.put("sensor", String.valueOf(sensorId));
        logger.debug("Sensor Data: ID={}, Type={}, Value={}", sensorId, type, value);
        ThreadContext.remove("sensor");
    }
    
    /**
     * Log command execution events
     */
    public static void logCommand(Logger logger, String command, boolean success, String details) {
        ThreadContext.put("command", command);
        if (success) {
            logger.info("Command executed successfully: {}", details);
        } else {
            logger.warn("Command execution failed: {}", details);
        }
        ThreadContext.remove("command");
    }
    
    /**
     * Log checksum validation results
     */
    public static void logChecksumValidation(Logger logger, byte[] data, boolean valid, String details) {
        if (valid) {
            logger.debug("Checksum validation successful for data packet");
        } else {
            logger.warn("Checksum validation failed: {}", details);
        }
    }
    
    /**
     * Execute an operation with logging
     */
    public static <T> T executeWithLogging(
            Logger logger,
            String operation,
            Supplier<T> action,
            String successMessage,
            String failureMessage) {
        try {
            logger.debug("Starting operation: {}", operation);
            T result = action.get();
            logger.debug("Operation completed: {} - {}", operation, successMessage);
            return result;
        } catch (Exception e) {
            logError(logger, operation, failureMessage, e);
            throw e;
        }
    }
} 