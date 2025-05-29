package com.dashtech.smartfactory.model;

import java.nio.ByteBuffer;

/**
 * Represents a serial packet for both sensor data and control commands.
 * Packet structure:
 * - Header (2 bytes): 0xAABB for sensor data, 0xCCDD for control commands
 * - ID (1 byte): Sensor ID or Actuator ID
 * - Type (1 byte): Data type for sensors, command type for actuators
 * - Payload (4 bytes): Float value for sensors, unused for commands
 * - Checksum (1 byte): XOR of all previous bytes
 */
public class SerialPacket {
    // Packet Headers
    public static final short SENSOR_HEADER = (short) 0xAABB;
    public static final short COMMAND_HEADER = (short) 0xCCDD;

    // Data Types
    public static class DataType {
        public static final byte TEMPERATURE = 0x01;
        public static final byte PRESSURE = 0x02;
    }

    // Command Types
    public static class CommandType {
        public static final byte OFF = 0x00;
        public static final byte ON = 0x01;
    }

    // Packet sizes
    public static final int SENSOR_PACKET_SIZE = 9; // 2(header) + 1(id) + 1(type) + 4(payload) + 1(checksum)
    public static final int COMMAND_PACKET_SIZE = 5; // 2(header) + 1(id) + 1(type) + 1(checksum)

    private short header;
    private byte id;
    private byte type;
    private float payload;  // Only used for sensor packets
    private byte checksum;

    // Constructor for sensor packets
    public static SerialPacket createSensorPacket(byte sensorId, byte dataType, float value) {
        SerialPacket packet = new SerialPacket();
        packet.header = SENSOR_HEADER;
        packet.id = sensorId;
        packet.type = dataType;
        packet.payload = value;
        packet.checksum = packet.calculateChecksum();
        return packet;
    }

    // Constructor for command packets
    public static SerialPacket createCommandPacket(byte actuatorId, byte commandType) {
        SerialPacket packet = new SerialPacket();
        packet.header = COMMAND_HEADER;
        packet.id = actuatorId;
        packet.type = commandType;
        packet.checksum = packet.calculateChecksum();
        return packet;
    }

    // Convert packet to byte array
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(isSensorPacket() ? SENSOR_PACKET_SIZE : COMMAND_PACKET_SIZE);
        buffer.putShort(header);
        buffer.put(id);
        buffer.put(type);
        if (isSensorPacket()) {
            buffer.putFloat(payload);
        }
        buffer.put(checksum);
        return buffer.array();
    }

    // Parse byte array to packet
    public static SerialPacket fromBytes(byte[] data) throws IllegalArgumentException {
        if (data.length < COMMAND_PACKET_SIZE) {
            throw new IllegalArgumentException("Packet too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        SerialPacket packet = new SerialPacket();
        
        // Read header
        packet.header = buffer.getShort();
        
        // Validate packet size based on header
        if (packet.header == SENSOR_HEADER && data.length < SENSOR_PACKET_SIZE) {
            throw new IllegalArgumentException("Sensor packet too short");
        }

        // Read common fields
        packet.id = buffer.get();
        packet.type = buffer.get();

        // Read payload for sensor packets
        if (packet.header == SENSOR_HEADER) {
            packet.payload = buffer.getFloat();
        }

        // Read checksum
        packet.checksum = buffer.get();

        // Validate checksum
        byte calculatedChecksum = packet.calculateChecksum();
        if (calculatedChecksum != packet.checksum) {
            throw new IllegalArgumentException(
                String.format("Checksum mismatch: expected %02X, got %02X", 
                    calculatedChecksum, packet.checksum));
        }

        return packet;
    }

    // Calculate checksum (XOR of all previous bytes)
    private byte calculateChecksum() {
        ByteBuffer buffer = ByteBuffer.allocate(isSensorPacket() ? SENSOR_PACKET_SIZE - 1 : COMMAND_PACKET_SIZE - 1);
        buffer.putShort(header);
        buffer.put(id);
        buffer.put(type);
        if (isSensorPacket()) {
            buffer.putFloat(payload);
        }
        
        byte checksum = 0;
        for (byte b : buffer.array()) {
            checksum ^= b;
        }
        return checksum;
    }

    private boolean isSensorPacket() {
        return header == SENSOR_HEADER;
    }

    // Getters
    public short getHeader() { return header; }
    public byte getId() { return id; }
    public byte getType() { return type; }
    public float getPayload() { return payload; }
    public byte getChecksum() { return checksum; }

    @Override
    public String toString() {
        if (isSensorPacket()) {
            return String.format("Sensor Packet [ID: %02X, Type: %02X, Value: %f]", id, type, payload);
        } else {
            return String.format("Command Packet [ID: %02X, Type: %02X]", id, type);
        }
    }
}