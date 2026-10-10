package com.example.usermanagement.exception;

/**
 * Thrown when a requested resource (e.g. a user) does not exist.
 * Mapped to HTTP 404 by {@code GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
