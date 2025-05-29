package com.dashtech.smartfactory.model;

/**
 * Record representing serial port configuration with immutable properties.
 * @param portName The name of the serial port
 * @param baudRate The baud rate for communication
 * @param dataBits Number of data bits
 * @param stopBits Number of stop bits
 * @param parity Parity setting (0=none, 1=odd, 2=even)
 */
public record SerialConfig(
    String portName,
    int baudRate,
    int dataBits,
    int stopBits,
    int parity
) {
    /**
     * Factory method to create a new SerialConfig instance with default settings
     */
    public static SerialConfig defaultConfig(String portName) {
        return new SerialConfig(portName, 9600, 8, 1, 0);
    }
    
    /**
     * Factory method to create a new SerialConfig instance with custom baud rate
     */
    public static SerialConfig withBaudRate(String portName, int baudRate) {
        return new SerialConfig(portName, baudRate, 8, 1, 0);
    }
} 