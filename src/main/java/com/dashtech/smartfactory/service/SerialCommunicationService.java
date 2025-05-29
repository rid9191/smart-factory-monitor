package com.dashtech.smartfactory.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dashtech.smartfactory.model.ActuatorCommand;
import com.dashtech.smartfactory.util.LoggingUtil;
import com.fazecast.jSerialComm.SerialPort;

public class SerialCommunicationService implements AutoCloseable {
    private static final Logger logger = LogManager.getLogger(SerialCommunicationService.class);
    private SerialPort serialPort;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private SerialDataCallback callback;
    
    public interface SerialDataCallback {
        void onDataReceived(String data);
        void onError(String error);
    }
    
    public void setCallback(SerialDataCallback callback) {
        this.callback = callback;
    }
    
    public List<String> getAvailablePorts() {
        return List.of(SerialPort.getCommPorts()).stream()
            .map(SerialPort::getSystemPortName)
            .toList();
    }
    
    public boolean connect(String portName, int baudRate) {
        if (connected.get()) {
            disconnect();
        }
        
        try {
            serialPort = SerialPort.getCommPort(portName);
            serialPort.setBaudRate(baudRate);
            serialPort.setNumDataBits(8);
            serialPort.setNumStopBits(1);
            serialPort.setParity(SerialPort.NO_PARITY);
            
            if (!serialPort.openPort()) {
                LoggingUtil.logError(logger, "Serial Connect", 
                    "Failed to open port " + portName, new Exception("Port open failed"));
                return false;
            }
            
            // Set up the data listener
            serialPort.addDataListener(new com.fazecast.jSerialComm.SerialPortDataListener() {
                @Override
                public int getListeningEvents() {
                    return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
                }
                
                @Override
                public void serialEvent(com.fazecast.jSerialComm.SerialPortEvent event) {
                    if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                        return;
                    }
                    
                    byte[] buffer = new byte[serialPort.bytesAvailable()];
                    int numRead = serialPort.readBytes(buffer, buffer.length);
                    
                    if (numRead > 0 && callback != null) {
                        String data = new String(buffer, 0, numRead).trim();
                        LoggingUtil.logSerialEvent(logger, "DATA_RECEIVED", portName, data);
                        callback.onDataReceived(data);
                    }
                }
            });
            
            connected.set(true);
            LoggingUtil.logSerialEvent(logger, "CONNECT", portName, 
                String.format("Connected at %d baud", baudRate));
            return true;
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Serial Connect", 
                "Error connecting to port " + portName, e);
            return false;
        }
    }
    
    public boolean sendCommand(ActuatorCommand command) {
        if (!connected.get() || serialPort == null) {
            LoggingUtil.logSerialEvent(logger, "SEND_ERROR", getCurrentPortName(), 
                "Not connected to serial port");
            return false;
        }
        
        try {
            byte[] data = command.toBinary();
            int written = serialPort.writeBytes(data, data.length);
            
            if (written == data.length) {
                LoggingUtil.logSerialEvent(logger, "SEND_SUCCESS", getCurrentPortName(), 
                    String.format("Command sent: %s", command.command()));
                return true;
            } else {
                LoggingUtil.logSerialEvent(logger, "SEND_ERROR", getCurrentPortName(), 
                    String.format("Failed to write all bytes: %d/%d", written, data.length));
                return false;
            }
        } catch (Exception e) {
            LoggingUtil.logError(logger, "Serial Send", 
                "Error sending command: " + command.command(), e);
            return false;
        }
    }
    
    public void disconnect() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.removeDataListener();
            serialPort.closePort();
            LoggingUtil.logSerialEvent(logger, "DISCONNECT", getCurrentPortName(), 
                "Port closed");
        }
        connected.set(false);
        serialPort = null;
    }
    
    public boolean isConnected() {
        return connected.get() && serialPort != null && serialPort.isOpen();
    }
    
    public String getCurrentPortName() {
        return serialPort != null ? serialPort.getSystemPortName() : "Not Connected";
    }
    
    @Override
    public void close() {
        disconnect();
    }
} 