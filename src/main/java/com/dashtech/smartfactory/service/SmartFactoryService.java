package com.dashtech.smartfactory.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dashtech.smartfactory.model.DatabaseConfig;
import com.dashtech.smartfactory.model.SensorData;
import com.dashtech.smartfactory.util.LoggingUtil;
import com.dashtech.smartfactory.websocket.SmartFactoryWebSocket;

@WebListener
public class SmartFactoryService implements ServletContextListener {
    private static final Logger logger = LogManager.getLogger(SmartFactoryService.class);
    private static DatabaseService databaseService;
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            String dbPath = System.getProperty("catalina.base") + "/data/smartfactory";
            Path path = Paths.get(dbPath);
            databaseService = DatabaseService.getInstance(DatabaseConfig.createDefault(path));
            SmartFactoryWebSocket.setDatabaseService(dbPath);
            
            LoggingUtil.logStartup(logger, "SmartFactoryService", Map.of(
                "dbPath", dbPath,
                "status", "initialized"
            ));
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Context Initialization", "Failed to initialize SmartFactoryService", e);
            throw new RuntimeException("Failed to initialize SmartFactoryService", e);
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            if (databaseService != null) {
                databaseService.close();
            }
            LoggingUtil.logShutdown(logger, "SmartFactoryService", Map.of("status", "shutdown_complete"));
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Context Destruction", "Error during shutdown", e);
        }
    }
    
    public void processSerialData(String data) {
        try {
            // Parse the sensor data format: Sensor[XX] Type[XX] Value[XX.XX]
            String[] parts = data.split("\\] ");
            int sensorId = Integer.parseInt(parts[0].substring(7), 16);
            String type = parts[1].substring(5);
            double value = Double.parseDouble(parts[2].substring(6, parts[2].length() - 1));
            
            SensorData sensorData = new SensorData(
                sensorId,
                Integer.parseInt(type, 16) == 1 ? "Temperature" : "Pressure",
                value,
                new Timestamp(System.currentTimeMillis()).toInstant()
            );
            
            databaseService.storeSensorData(sensorData);
            LoggingUtil.logSensorData(logger, sensorId, sensorData.type(), value);
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Serial Data Processing", "Error processing serial data: " + data, e);
        }
    }
}
