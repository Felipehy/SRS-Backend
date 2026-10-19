package com.siurbinfo.srs.exception;

public class SameDepartureAndDestinationException extends RuntimeException {
    public SameDepartureAndDestinationException(String message) {
        super(message);
    }
}
