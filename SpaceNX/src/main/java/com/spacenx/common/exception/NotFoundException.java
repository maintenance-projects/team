package com.spacenx.common.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String entityName, Object identifier) {
        super(entityName + " not found with identifier: " + identifier);
    }
}
