package com.dashtech.smartfactory.service;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;

import com.dashtech.smartfactory.logging.LoggerFactory;
import com.dashtech.smartfactory.model.SensorReading;

public class VirtualThreadSerialService {

    private static final Logger logger = LoggerFactory.getLogger(VirtualThreadSerialService.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(2); // One for listener, one for processing
    private final Random random = new Random();

    public void startSerialListener() {
        // Start serial data listener
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Mock serial data generation
                    generateMockSensorData();
                    Thread.sleep(2000); // Sleep for 2 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        logger.info("Serial listener started");
    }

    private void generateMockSensorData() {
        // Generate random sensor data
        var sensorData = switch (random.nextInt(3)) {
            case 0 -> new SensorReading(1, "Temperature", 20.0 + random.nextDouble() * 10);
            case 1 -> new SensorReading(2, "Pressure", 100.0 + random.nextDouble() * 50);
            case 2 -> new SensorReading(3, "Humidity", 40.0 + random.nextDouble() * 20);
            default -> throw new IllegalStateException("Unexpected value");
        };

        // Process with thread pool
        executor.submit(() -> processSensorData(sensorData));
    }

    private void processSensorData(SensorReading sensorData) {
        try {
            // Simulate some processing time
            Thread.sleep(100);
            
            // Log the processed sensor data
            logger.info("Processed sensor reading - ID: {}, Type: {}, Value: {}", 
                sensorData.sensorId(), 
                sensorData.dataType(), 
                sensorData.value());
                
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Sensor data processing interrupted", e);
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
