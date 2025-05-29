package com.smartfactory.service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

public class SerialCommunicationService {
    private static final Logger logger = LoggerFactory.getLogger(SerialCommunicationService.class);
    private SerialPort serialPort;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread readThread;
    private SerialDataCallback callback;

    public interface SerialDataCallback {
        void onDataReceived(String data);
        void onError(String error);
    }

    public SerialCommunicationService() {
    }

    public List<String> getAvailablePorts() {
        return Arrays.stream(SerialPort.getCommPorts())
                    .map(SerialPort::getSystemPortName)
                    .toList();
    }

    public boolean connect(String portName, int baudRate) {
        // Close any existing connection
        disconnect();

        // Find the port by name
        serialPort = Arrays.stream(SerialPort.getCommPorts())
                          .filter(port -> port.getSystemPortName().equals(portName))
                          .findFirst()
                          .orElse(null);

        if (serialPort == null) {
            logger.error("Port {} not found", portName);
            return false;
        }

        // Configure the port
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);
        
        // Open the port
        if (!serialPort.openPort()) {
            logger.error("Failed to open port {}", portName);
            return false;
        }

        // Start reading data
        startReading();
        return true;
    }

    public void disconnect() {
        isRunning.set(false);
        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
            serialPort = null;
        }
    }

    public boolean sendCommand(String command) {
        if (serialPort == null || !serialPort.isOpen()) {
            logger.error("Serial port is not open");
            return false;
        }

        try {
            byte[] data = command.getBytes();
            int bytesWritten = serialPort.writeBytes(data, data.length);
            return bytesWritten == data.length;
        } catch (Exception e) {
            logger.error("Error sending command: {}", e.getMessage());
            return false;
        }
    }

    public void setCallback(SerialDataCallback callback) {
        this.callback = callback;
    }

    private void startReading() {
        isRunning.set(true);
        readThread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            StringBuilder messageBuilder = new StringBuilder();

            while (isRunning.get()) {
                try {
                    if (serialPort.bytesAvailable() > 0) {
                        int numRead = serialPort.readBytes(buffer, Math.min(buffer.length, serialPort.bytesAvailable()));
                        
                        if (numRead > 0) {
                            String received = new String(buffer, 0, numRead);
                            messageBuilder.append(received);

                            // Process complete messages (assuming \n as delimiter)
                            int newlineIndex;
                            while ((newlineIndex = messageBuilder.indexOf("\n")) != -1) {
                                String message = messageBuilder.substring(0, newlineIndex).trim();
                                if (!message.isEmpty() && callback != null) {
                                    callback.onDataReceived(message);
                                }
                                messageBuilder.delete(0, newlineIndex + 1);
                            }
                        }
                    }
                    Thread.sleep(10); // Small delay to prevent CPU hogging
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.error("Error reading from serial port: {}", e.getMessage());
                    if (callback != null) {
                        callback.onError("Error reading from serial port: " + e.getMessage());
                    }
                    break;
                }
            }
        });
        readThread.setDaemon(true);
        readThread.start();
    }

    public boolean isConnected() {
        return serialPort != null && serialPort.isOpen();
    }

    public String getCurrentPortName() {
        return serialPort != null ? serialPort.getSystemPortName() : null;
    }
} 