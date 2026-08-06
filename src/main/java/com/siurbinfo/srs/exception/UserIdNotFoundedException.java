package com.siurbinfo.srs.exception;

// Lancada quando o id de usuario informado nao existe na base.
public class UserIdNotFoundedException extends RuntimeException {
    public UserIdNotFoundedException(String message) {
        super(message);
    }
}
