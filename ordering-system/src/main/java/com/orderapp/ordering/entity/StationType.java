package com.orderapp.ordering.entity;

public enum StationType {
    TABLE,
    UMBRELLA,
    SUNBED,
    LOUNGE,
    VIP_AREA,
    SERVICE_POINT;

    public String toDatabaseValue() {
        return switch (this) {
            case VIP_AREA -> "VIP";
            case SERVICE_POINT -> "GENERIC";
            default -> name();
        };
    }

    public static StationType fromDatabaseValue(String value) {
        if (value == null) {
            return SERVICE_POINT;
        }

        return switch (value.toUpperCase()) {
            case "TABLE" -> TABLE;
            case "UMBRELLA" -> UMBRELLA;
            case "SUNBED" -> SUNBED;
            case "LOUNGE", "ROOM" -> LOUNGE;
            case "VIP", "VIP_AREA" -> VIP_AREA;
            default -> SERVICE_POINT;
        };
    }
}