package com.siurbinfo.srs.exception;

// Lancada quando a quantidade de pessoas informada na reserva e invalida (ex: excede a capacidade da sala).
public class InvalidPeopleCountException extends RuntimeException {
    public InvalidPeopleCountException(String message) {
        super(message);
    }
}
