package com.dashtech.smartfactory.websocket;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dashtech.smartfactory.model.ActuatorCommand;
import com.dashtech.smartfactory.model.DatabaseConfig;
import com.dashtech.smartfactory.service.DatabaseService;
import com.dashtech.smartfactory.service.SerialCommunicationService;
import com.dashtech.smartfactory.util.LoggingUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ServerEndpoint("/ws/smartfactory")
public class SmartFactoryWebSocket {
    private static final Logger logger = LogManager.getLogger(SmartFactoryWebSocket.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Map to store WebSocket sessions
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    
    // Map to store serial connections for each session
    private static final Map<String, SerialCommunicationService> serialConnections = new ConcurrentHashMap<>();
    
    private static DatabaseService databaseService;

    public static void setDatabaseService(String dbPath) {
        Path path = Paths.get(dbPath);
        databaseService = DatabaseService.getInstance(DatabaseConfig.createDefault(path));
    }

    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        LoggingUtil.logWebSocketEvent(logger, "OPEN", session.getId(), "New WebSocket connection established");
        
        try {
            // Create a new serial service for this session
            SerialCommunicationService serialService = new SerialCommunicationService();
            serialConnections.put(session.getId(), serialService);
            
            // Set up callback for this connection
            serialService.setCallback(new SerialCommunicationService.SerialDataCallback() {
                @Override
                public void onDataReceived(String data) {
                    sendSerialData(session, data);
                }
                
                @Override
                public void onError(String error) {
                    sendError(session, error);
                }
            });
            
            sendConnectionStatus(session);
        } catch (Exception e) {
            LoggingUtil.logError(logger, "WebSocket Open", "Failed to initialize session", e);
            sendError(session, "Failed to initialize session: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        // Clean up the serial connection for this session
        SerialCommunicationService serialService = serialConnections.remove(session.getId());
        if (serialService != null) {
            serialService.disconnect();
        }
        sessions.remove(session.getId());
        LoggingUtil.logWebSocketEvent(logger, "CLOSE", session.getId(), "WebSocket connection closed");
    }

    @OnError
    public void onError(Session session, Throwable error) {
        LoggingUtil.logError(logger, "WebSocket Error", "Error in session " + session.getId(), error);
        // Clean up on error
        SerialCommunicationService serialService = serialConnections.remove(session.getId());
        if (serialService != null) {
            serialService.disconnect();
        }
        sessions.remove(session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        LoggingUtil.logWebSocketEvent(logger, "MESSAGE", session.getId(), "Received: " + message);
        
        try {
            JsonNode json = objectMapper.readTree(message);
            String type = json.get("type").asText();
            
            SerialCommunicationService serialService = serialConnections.get(session.getId());
            if (serialService == null) {
                sendError(session, "Serial service not initialized");
                return;
            }

            switch (type) {
                case "command" -> handleCommand(json, session, serialService);
                case "getports" -> handleGetPorts(session, serialService);
                case "connect" -> handleConnect(json, session, serialService);
                case "disconnect" -> handleDisconnect(session, serialService);
                default -> logger.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Message Processing", "Error processing message: " + message, e);
            sendError(session, "Error processing message: " + e.getMessage());
        }
    }

    private void handleCommand(JsonNode json, Session session, SerialCommunicationService serialService) {
        try {
            if (!serialService.isConnected()) {
                sendError(session, "Serial port not connected");
                return;
            }

            ActuatorCommand command;
            if (json.has("command")) {
                // Legacy format
                String cmd = json.get("command").asText();
                command = ActuatorCommand.create(0, cmd); // Default actuator ID
            } else {
                // New format
                int actuatorId = json.get("actuatorId").asInt();
                String cmd = json.get("command").asText().toUpperCase();
                command = ActuatorCommand.create(actuatorId, cmd);
            }

            if (serialService.sendCommand(command)) {
                databaseService.logCommand(command, true, null, session.getId());
                LoggingUtil.logCommand(logger, command.command(), true, 
                    String.format("Actuator: %d, Session: %s", command.actuatorId(), session.getId()));
            } else {
                String error = "Failed to send command: " + command.command();
                databaseService.logCommand(command, false, error, session.getId());
                LoggingUtil.logCommand(logger, command.command(), false, error);
                sendError(session, error);
            }
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Command Handling", "Error executing command", e);
            sendError(session, "Error executing command: " + e.getMessage());
        }
    }

    private void handleGetPorts(Session session, SerialCommunicationService serialService) {
        try {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("type", "ports");
            response.putPOJO("ports", serialService.getAvailablePorts());
            sendMessage(session, response.toString());
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Get Ports", "Error getting available ports", e);
            sendError(session, "Error getting available ports: " + e.getMessage());
        }
    }

    private void handleConnect(JsonNode json, Session session, SerialCommunicationService serialService) {
        try {
            String port = json.get("port").asText();
            int baudRate = json.get("baudRate").asInt(9600);

            if (serialService.connect(port, baudRate)) {
                LoggingUtil.logWebSocketEvent(logger, "CONNECT", session.getId(), 
                    String.format("Connected to port %s at %d baud", port, baudRate));
                sendConnectionStatus(session);
            } else {
                sendError(session, "Failed to connect to port: " + port);
            }
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Connect", "Error connecting to serial port", e);
            sendError(session, "Error connecting to serial port: " + e.getMessage());
        }
    }

    private void handleDisconnect(Session session, SerialCommunicationService serialService) {
        try {
            serialService.disconnect();
            LoggingUtil.logWebSocketEvent(logger, "DISCONNECT", session.getId(), "Disconnected from serial port");
            sendConnectionStatus(session);
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Disconnect", "Error disconnecting from serial port", e);
            sendError(session, "Error disconnecting from serial port: " + e.getMessage());
        }
    }

    private void sendSerialData(Session session, String data) {
        try {
            // Parse the sensor data format: Sensor[XX] Type[XX] Value[XX.XX]
            if (data.startsWith("Sensor[")) {
                final String[] parts = data.split("\\] ");
                final int sensorId = Integer.parseInt(parts[0].substring(7), 16);
                final String type = parts[1].substring(5);
                final double value = Double.parseDouble(parts[2].substring(6, parts[2].length() - 1));

                ObjectNode message = objectMapper.createObjectNode();
                message.put("sensorId", sensorId);
                message.put("type", Integer.parseInt(type, 16) == 1 ? "Temperature" : "Pressure");
                message.put("value", value);
                
                sendMessage(session, message.toString());
            }
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Serial Data Processing", "Error processing serial data: " + data, e);
        }
    }

    private void sendConnectionStatus(Session session) {
        try {
            SerialCommunicationService serialService = serialConnections.get(session.getId());
            ObjectNode status = objectMapper.createObjectNode();
            status.put("type", "connection");
            status.put("connected", serialService != null && serialService.isConnected());
            if (serialService != null && serialService.isConnected()) {
                status.put("port", serialService.getCurrentPortName());
            }
            sendMessage(session, status.toString());
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Connection Status", "Error sending connection status", e);
        }
    }

    private static void sendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
            LoggingUtil.logWebSocketEvent(logger, "SEND", session.getId(), "Sent: " + message);
        } catch (IOException e) {
            LoggingUtil.logError(logger, "Message Send", "Failed to send message to session " + session.getId(), e);
        }
    }

    private static void sendError(Session session, String error) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("type", "error");
        message.put("message", error);
        sendMessage(session, message.toString());
    }
}


