package com.siurbinfo.srs.exception;

public class ReservationNotFoundedException extends RuntimeException {
    public ReservationNotFoundedException(String message) {
        super(message);
    }
}
