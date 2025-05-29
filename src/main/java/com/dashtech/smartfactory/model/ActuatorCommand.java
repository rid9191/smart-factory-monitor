package com.dashtech.smartfactory.model;

import java.time.Instant;

/**
 * Record representing an actuator command with immutable properties.
 * @param actuatorId The unique identifier of the actuator
 * @param command The command to execute (e.g., "ON", "OFF")
 * @param timestamp The time when the command was issued
 */
public record ActuatorCommand(
    int actuatorId,
    String command,
    Instant timestamp
) {
    private static final byte PACKET_START = (byte) 0xAA;
    private static final byte PACKET_END = (byte) 0x55;
    private static final byte COMMAND_PACKET_TYPE = 0x02;
    
    /**
     * Factory method to create a new ActuatorCommand instance with current timestamp
     */
    public static ActuatorCommand create(int actuatorId, String command) {
        return new ActuatorCommand(actuatorId, command.toUpperCase(), Instant.now());
    }
    
    /**
     * Converts the command to its binary representation
     * @return 1 for "ON", 0 for "OFF"
     */
    public byte[] toBinary() {
        byte commandValue = switch (command) {
            case "ON" -> 0x01;
            case "OFF" -> 0x00;
            default -> throw new IllegalArgumentException("Invalid command: " + command);
        };
        
        // Packet format: START(1) + TYPE(1) + ACTUATOR_ID(1) + COMMAND(1) + CHECKSUM(1) + END(1)
        byte[] packet = new byte[6];
        packet[0] = PACKET_START;
        packet[1] = COMMAND_PACKET_TYPE;
        packet[2] = (byte) actuatorId;
        packet[3] = commandValue;
        packet[4] = calculateChecksum(packet, 1, 3); // Checksum over TYPE + ACTUATOR_ID + COMMAND
        packet[5] = PACKET_END;
        
        return packet;
    }
    
    private static byte calculateChecksum(byte[] data, int start, int length) {
        byte sum = 0;
        for (int i = start; i < start + length; i++) {
            sum ^= data[i]; // XOR checksum
        }
        return sum;
    }
} 