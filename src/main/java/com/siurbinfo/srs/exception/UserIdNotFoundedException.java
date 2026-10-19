package com.siurbinfo.srs.exception;

public class UserIdNotFoundedException extends RuntimeException {
    public UserIdNotFoundedException(String message) {
        super(message);
    }
}
