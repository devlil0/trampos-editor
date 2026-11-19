package com.devlil0.tramposeditor.exception;

public final class AuthRateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public AuthRateLimitException(long retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
