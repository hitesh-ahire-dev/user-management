package com.example.usermanagement.exception;

/**
 * Thrown when creating/updating a resource would violate a uniqueness rule
 * (e.g. an email already in use). Mapped to HTTP 409 by the handler.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
