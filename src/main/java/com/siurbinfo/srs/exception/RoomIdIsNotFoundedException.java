package com.siurbinfo.srs.exception;

// Lancada quando o id de sala informado nao existe na base.
public class RoomIdIsNotFoundedException extends RuntimeException {
    public RoomIdIsNotFoundedException(String message) {
        super(message);
    }
}
