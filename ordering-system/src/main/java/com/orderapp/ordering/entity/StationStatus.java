package com.orderapp.ordering.entity;

public enum StationStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    ORDERING_DISABLED,
    CLOSED;

    public static StationStatus fromDatabaseValue(String value) {
        if (value == null) {
            return AVAILABLE;
        }

        try {
            return StationStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AVAILABLE;
        }
    }
}