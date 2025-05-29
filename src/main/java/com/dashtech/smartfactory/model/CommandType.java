package com.dashtech.smartfactory.model;

public enum CommandType {
    ON((byte) 0x01, "ON"),
    OFF((byte) 0x00, "OFF"),
    UNKNOWN((byte) 0xFF, "UNKNOWN");

    private final byte code;
    private final String name;

    CommandType(byte code, String name) {
        this.code = code;
        this.name = name;
    }

    public byte getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static CommandType fromCode(byte code) {
        for (CommandType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return UNKNOWN;
    }

    public static CommandType fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
} 