package com.example.trace.security;

/**
 * Token blacklist storage exception.
 *
 * <p>The Redis-backed blacklist is a security dependency. When it is unavailable or cannot
 * persist a revocation, callers must fail closed instead of silently allowing the request.</p>
 */
public class TokenStoreException extends RuntimeException {

    private final String operation;

    public TokenStoreException(String operation, String message) {
        super(message);
        this.operation = operation;
    }

    public TokenStoreException(String operation, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
