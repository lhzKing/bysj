package com.example.trace.service.impl.support;

public class TraceOptimisticLockException extends RuntimeException {
    public TraceOptimisticLockException(String message) {
        super(message);
    }
}
