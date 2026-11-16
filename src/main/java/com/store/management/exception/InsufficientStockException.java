package com.store.management.exception;

/** Thrown when a stock decrease would make the quantity negative (HTTP 409). */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productId, int available, int requested) {
        super("Insufficient stock for product " + productId
                + ": available " + available + ", requested " + requested);
    }
}
