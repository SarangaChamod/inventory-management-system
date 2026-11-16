package com.store.management.exception;

/** Thrown when creating or updating a resource would violate a uniqueness rule (HTTP 409). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
