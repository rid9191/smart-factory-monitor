package com.dashtech.smartfactory.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dashtech.smartfactory.model.SerialPacket;

public class PacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(PacketHandler.class);
    private final List<Byte> buffer = new ArrayList<>();
    private PacketCallback callback;

    public interface PacketCallback {
        void onSensorData(byte sensorId, byte dataType, float value);
        void onCommandResponse(byte actuatorId, byte commandType);
        void onError(String error);
    }

    public PacketHandler(PacketCallback callback) {
        this.callback = callback;
    }

    public void processIncomingData(byte[] data) {
        // Add new data to buffer
        for (byte b : data) {
            buffer.add(b);
        }

        // Process complete packets
        while (buffer.size() >= 2) {  // At least header size
            // Check for sensor packet header
            if (checkHeader(SerialPacket.SENSOR_HEADER)) {
                if (buffer.size() >= SerialPacket.SENSOR_PACKET_SIZE) {
                    processSensorPacket();
                } else {
                    break; // Wait for more data
                }
            }
            // Check for command packet header
            else if (checkHeader(SerialPacket.COMMAND_HEADER)) {
                if (buffer.size() >= SerialPacket.COMMAND_PACKET_SIZE) {
                    processCommandPacket();
                } else {
                    break; // Wait for more data
                }
            }
            // No valid header found, remove first byte
            else {
                buffer.remove(0);
            }
        }
    }

    private boolean checkHeader(short header) {
        if (buffer.size() < 2) return false;
        short packetHeader = (short) ((buffer.get(0) << 8) | (buffer.get(1) & 0xFF));
        return packetHeader == header;
    }

    private void processSensorPacket() {
        try {
            // Convert buffer to byte array
            byte[] packetData = new byte[SerialPacket.SENSOR_PACKET_SIZE];
            for (int i = 0; i < SerialPacket.SENSOR_PACKET_SIZE; i++) {
                packetData[i] = buffer.get(i);
            }

            // Parse packet
            SerialPacket packet = SerialPacket.fromBytes(packetData);
            
            // Notify callback
            callback.onSensorData(packet.getId(), packet.getType(), packet.getPayload());

            // Remove processed bytes
            for (int i = 0; i < SerialPacket.SENSOR_PACKET_SIZE; i++) {
                buffer.remove(0);
            }

        } catch (Exception e) {
            logger.error("Error processing sensor packet: {}", e.getMessage());
            callback.onError("Failed to process sensor packet: " + e.getMessage());
            // Remove invalid packet
            buffer.remove(0);
        }
    }

    private void processCommandPacket() {
        try {
            // Convert buffer to byte array
            byte[] packetData = new byte[SerialPacket.COMMAND_PACKET_SIZE];
            for (int i = 0; i < SerialPacket.COMMAND_PACKET_SIZE; i++) {
                packetData[i] = buffer.get(i);
            }

            // Parse packet
            SerialPacket packet = SerialPacket.fromBytes(packetData);
            
            // Notify callback
            callback.onCommandResponse(packet.getId(), packet.getType());

            // Remove processed bytes
            for (int i = 0; i < SerialPacket.COMMAND_PACKET_SIZE; i++) {
                buffer.remove(0);
            }

        } catch (Exception e) {
            logger.error("Error processing command packet: {}", e.getMessage());
            callback.onError("Failed to process command packet: " + e.getMessage());
            // Remove invalid packet
            buffer.remove(0);
        }
    }

    public byte[] createSensorPacket(byte sensorId, byte dataType, float value) {
        return SerialPacket.createSensorPacket(sensorId, dataType, value).toBytes();
    }

    public byte[] createCommandPacket(byte actuatorId, byte commandType) {
        return SerialPacket.createCommandPacket(actuatorId, commandType).toBytes();
    }
} 