package com.siurbinfo.srs.exception;

public class StartTimeIsNotBeforeEndTimeException extends RuntimeException {
    public StartTimeIsNotBeforeEndTimeException(String message) {
        super(message);
    }
}
