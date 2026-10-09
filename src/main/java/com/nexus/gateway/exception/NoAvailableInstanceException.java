package com.nexus.gateway.exception;

public class NoAvailableInstanceException extends RuntimeException {
    public NoAvailableInstanceException() {
        super("No available service");
    }

    public NoAvailableInstanceException(String message) {
        super(message);
    }
}
