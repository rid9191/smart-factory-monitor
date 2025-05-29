package com.dashtech.smartfactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dashtech.smartfactory.websocket.SmartFactoryWebSocket;

@WebListener
public class SmartFactoryApplication implements ServletContextListener {
    private static final Logger logger = LogManager.getLogger(SmartFactoryApplication.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initializing Smart Factory Application");
        
        try {
            // Initialize database service with absolute path
            String dbPath = "E:/apache-tomcat-9/data/smartfactory";
            logger.info("Using database path: {}", dbPath);
            
            // Initialize WebSocket service with database path
            SmartFactoryWebSocket.setDatabaseService(dbPath);
            logger.info("WebSocket service initialized");

            logger.info("Smart Factory Application initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize Smart Factory Application", e);
            throw new RuntimeException("Application initialization failed", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Smart Factory Application shutdown complete");
    }
}
