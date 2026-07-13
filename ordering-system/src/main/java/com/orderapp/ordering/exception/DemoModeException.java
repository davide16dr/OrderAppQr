package com.orderapp.ordering.exception;

public class DemoModeException extends RuntimeException {
    public DemoModeException() {
        super("Questa operazione non è disponibile nell'account demo.");
    }
}
