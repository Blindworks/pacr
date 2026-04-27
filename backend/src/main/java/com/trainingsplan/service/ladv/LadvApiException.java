package com.trainingsplan.service.ladv;

/** Thrown by {@code LadvApiClient} on any HTTP / parse failure. */
public class LadvApiException extends RuntimeException {
    private final int statusCode;

    public LadvApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public LadvApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public int getStatusCode() { return statusCode; }
}
