package com.dashtech.smartfactory.model;

public enum SensorType {
    TEMPERATURE((byte) 0x01, "Temperature", "°C"),
    PRESSURE((byte) 0x02, "Pressure", "kPa"),
    HUMIDITY((byte) 0x03, "Humidity", "%"),
    UNKNOWN((byte) 0x00, "Unknown", "");

    private final byte code;
    private final String name;
    private final String unit;

    SensorType(byte code, String name, String unit) {
        this.code = code;
        this.name = name;
        this.unit = unit;
    }

    public byte getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getUnit() {
        return unit;
    }

    public static SensorType fromCode(byte code) {
        for (SensorType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UNKNOWN;
    }
} 