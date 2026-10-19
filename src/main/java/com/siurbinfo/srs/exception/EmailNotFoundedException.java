package com.siurbinfo.srs.exception;

public class EmailNotFoundedException extends RuntimeException {
    public EmailNotFoundedException(String message) {
        super(message);
    }
}
