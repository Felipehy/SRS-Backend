package com.siurbinfo.srs.exception;

public class FileNotExistInEfsException extends RuntimeException {
    public FileNotExistInEfsException(String message) {
        super(message);
    }
}
