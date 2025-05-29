package com.dashtech.smartfactory.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.h2.jdbcx.JdbcConnectionPool;

import com.dashtech.smartfactory.model.ActuatorCommand;
import com.dashtech.smartfactory.model.CommandLog;
import com.dashtech.smartfactory.model.DatabaseConfig;
import com.dashtech.smartfactory.model.SensorData;
import com.dashtech.smartfactory.util.LoggingUtil;

@WebListener
public class DatabaseService implements AutoCloseable, ServletContextListener {
    private static final Logger logger = LogManager.getLogger(DatabaseService.class);
    private static final AtomicReference<DatabaseService> instance = new AtomicReference<>();
    
    private JdbcConnectionPool dataSource;
    private DatabaseConfig config;
    
    // Required no-argument constructor for servlet container
    public DatabaseService() {
        // Default configuration will be set in contextInitialized
    }
    
    private DatabaseService(DatabaseConfig config) {
        initialize(config);
    }
    
    private void initialize(DatabaseConfig config) {
        this.config = config;
        this.dataSource = JdbcConnectionPool.create(config.url(), config.username(), config.password());
        this.dataSource.setMaxConnections(config.maxPoolSize());
        
        Map<String, String> startupDetails = new HashMap<>();
        startupDetails.put("url", config.url());
        startupDetails.put("maxPoolSize", String.valueOf(config.maxPoolSize()));
        startupDetails.put("minPoolSize", String.valueOf(config.minPoolSize()));
        LoggingUtil.logStartup(logger, "DatabaseService", startupDetails);
        
        initializeDatabase();
    }
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // Configure H2 to properly handle JVM shutdown
            System.setProperty("h2.exitOnShutdown", "true");
            
            String dbPath = System.getProperty("catalina.base") + "/data/smartfactory";
            Path path = Paths.get(dbPath);
            DatabaseConfig config = DatabaseConfig.createDefault(path);
            initialize(config);
            instance.set(this);
            logger.info("DatabaseService initialized via servlet context");
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Context Initialization", "Failed to initialize DatabaseService", e);
            throw new RuntimeException("Failed to initialize DatabaseService", e);
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            logger.info("Starting database shutdown sequence");
            
            // First set the instance to null to prevent new connections
            instance.set(null);
            
            // Close connection pool first
            if (dataSource != null) {
                try {
                    // Get current active connections
                    int activeConnections = ((JdbcConnectionPool) dataSource).getActiveConnections();
                    logger.info("Active connections before shutdown: {}", activeConnections);
                    
                    // Dispose the pool - this will close all active connections
                    ((JdbcConnectionPool) dataSource).dispose();
                    logger.info("Connection pool disposed successfully");
                } catch (Exception e) {
                    logger.error("Error disposing connection pool", e);
                } finally {
                    dataSource = null;
                }
            }
            
            // Attempt to shutdown the database engine
            try {
                // Get the database URL without the settings
                String baseUrl = config.url().split(";")[0];
                // Create a shutdown URL
                String shutdownUrl = baseUrl + ";SHUTDOWN=TRUE;SHUTDOWN_FORCE=TRUE";
                
                // Execute shutdown
                try (Connection conn = DriverManager.getConnection(shutdownUrl)) {
                    // Connection will fail as the database is shut down
                    logger.warn("Unexpected: shutdown connection succeeded");
                } catch (SQLException e) {
                    // This is expected - database is shut down
                    logger.info("Database shutdown completed successfully");
                }
            } catch (Exception e) {
                logger.error("Error during database shutdown", e);
            }
            
            logger.info("DatabaseService shutdown completed");
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Context Destruction", "Error during shutdown", e);
        }
    }
    
    public static DatabaseService getInstance(DatabaseConfig config) {
        return instance.updateAndGet(current -> {
            if (current == null || current.dataSource == null) {
                return new DatabaseService(config);
            }
            return current;
        });
    }
    
    private void initializeDatabase() {
        String dropCommandLogTable = "DROP TABLE IF EXISTS COMMAND_LOG";
        String dropSensorDataTable = "DROP TABLE IF EXISTS SENSOR_DATA";

        String createSensorDataTable = """
            CREATE TABLE IF NOT EXISTS SENSOR_DATA (
                ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                SENSOR_ID INT NOT NULL,
                DATA_TYPE VARCHAR(50) NOT NULL,
                "VALUE" DOUBLE NOT NULL,
                TIMESTAMP TIMESTAMP NOT NULL
            )
        """;

        String createCommandLogTable = """
           CREATE TABLE IF NOT EXISTS COMMAND_LOG (
                     ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                     ACTUATOR_ID INT NOT NULL,
                     COMMAND_LOG VARCHAR(50) NOT NULL,
                     SUCCESS BOOLEAN NOT NULL,
                     ERROR_MESSAGE VARCHAR(255),
                     "TIMESTAMP" TIMESTAMP NOT NULL,
                     OPERATOR_ID VARCHAR(50) NOT NULL
                 )
        """;

        String verifyTablesQuery = """
            SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE 
            FROM INFORMATION_SCHEMA.COLUMNS 
            WHERE TABLE_NAME IN ('COMMAND_LOG', 'SENSOR_DATA')
            ORDER BY TABLE_NAME, ORDINAL_POSITION
        """;
        
        String transactionId = UUID.randomUUID().toString();
        LoggingUtil.logTransaction(logger, transactionId, "START", "Initializing database tables");
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                //Drop Table if Exists
                try (PreparedStatement stmt = conn.prepareStatement(dropCommandLogTable)) {
                    stmt.execute();
                    LoggingUtil.logDatabaseOperation(logger, "DROP_TABLE", "DROPPED command_log table");
                }
                try (PreparedStatement stmt = conn.prepareStatement(dropSensorDataTable)) {
                    stmt.execute();
                    LoggingUtil.logDatabaseOperation(logger, "DROP_TABLE", "DROPPED sensor_data table");
                }

                // Create tables
                try (PreparedStatement stmt = conn.prepareStatement(createSensorDataTable)) {
                    stmt.execute();
                    LoggingUtil.logDatabaseOperation(logger, "CREATE_TABLE", "Created sensor_data table");
                }
                try (PreparedStatement stmt = conn.prepareStatement(createCommandLogTable)) {
                    stmt.execute();
                    LoggingUtil.logDatabaseOperation(logger, "CREATE_TABLE", "Created command_log table");
                }

                // Verify table structure
                try (PreparedStatement stmt = conn.prepareStatement(verifyTablesQuery)) {
                    var rs = stmt.executeQuery();
                    StringBuilder schema = new StringBuilder("Database Schema:\n");
                    String currentTable = null;
                    while (rs.next()) {
                        String tableName = rs.getString("table_name");
                        if (!tableName.equals(currentTable)) {
                            schema.append("\nTable: ").append(tableName).append("\n");
                            currentTable = tableName;
                        }
                        schema.append("  - ")
                              .append(rs.getString("column_name"))
                              .append(" (")
                              .append(rs.getString("data_type"))
                              .append(")\n");
                    }
                    logger.info(schema.toString());
                }

                conn.commit();
                LoggingUtil.logTransaction(logger, transactionId, "COMMIT", "Database tables initialized successfully");
            } catch (SQLException e) {
                conn.rollback();
                LoggingUtil.logTransaction(logger, transactionId, "ROLLBACK", "Failed to initialize database tables");
                LoggingUtil.logError(logger, "Database Initialization", "Failed to create tables", e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    public void storeSensorData(SensorData data) {
        String sql = """
            INSERT INTO SENSOR_DATA (
                SENSOR_ID, DATA_TYPE, "VALUE", TIMESTAMP
            ) VALUES (?, ?, ?, ?)
        """;
        
        String transactionId = UUID.randomUUID().toString();
        LoggingUtil.logTransaction(logger, transactionId, "START", "Storing sensor data");
        LoggingUtil.logSensorData(logger, data.sensorId(), data.type(), data.value());
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, data.sensorId());
                    stmt.setString(2, data.type());
                    stmt.setDouble(3, data.value());
                    stmt.setTimestamp(4, Timestamp.from(data.timestamp()));
                    stmt.executeUpdate();
                }
                conn.commit();
                LoggingUtil.logTransaction(logger, transactionId, "COMMIT", 
                    String.format("Stored sensor data: ID=%d, Type=%s", data.sensorId(), data.type()));
            } catch (SQLException e) {
                conn.rollback();
                LoggingUtil.logTransaction(logger, transactionId, "ROLLBACK", "Failed to store sensor data");
                LoggingUtil.logError(logger, "Store Sensor Data", "Database operation failed", e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database operation failed", e);
        }
    }
    
    public void logCommand(CommandLog log) {
        String sql = """
            INSERT INTO COMMAND_LOG (
                ACTUATOR_ID, COMMAND_LOG, SUCCESS, ERROR_MESSAGE, TIMESTAMP, OPERATOR_ID
            ) VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        String transactionId = UUID.randomUUID().toString();
        LoggingUtil.logTransaction(logger, transactionId, "START", "Logging command execution");
        LoggingUtil.logCommand(logger, log.command(), log.success(), 
            String.format("Actuator=%d, Operator=%s", log.actuatorId(), log.operatorId()));
        
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, log.actuatorId());
                    stmt.setString(2, log.command());
                    stmt.setBoolean(3, log.success());
                    stmt.setString(4, log.errorMessage());
                    stmt.setTimestamp(5, Timestamp.from(log.timestamp()));
                    stmt.setString(6, log.operatorId());
                    stmt.executeUpdate();
                }
                conn.commit();
                LoggingUtil.logTransaction(logger, transactionId, "COMMIT", "Command log stored successfully");
            } catch (SQLException e) {
                conn.rollback();
                LoggingUtil.logTransaction(logger, transactionId, "ROLLBACK", "Failed to store command log");
                LoggingUtil.logError(logger, "Log Command", "Database operation failed", e);
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database operation failed", e);
        }
    }
    
    public void logCommand(ActuatorCommand cmd, boolean success, String error, String operatorId) {
        CommandLog log = success ? 
            CommandLog.success(cmd, operatorId) : 
            CommandLog.failure(cmd, error, operatorId);
        logCommand(log);
    }
    
    @Override
    public void close() {
        contextDestroyed(null);
    }
}
