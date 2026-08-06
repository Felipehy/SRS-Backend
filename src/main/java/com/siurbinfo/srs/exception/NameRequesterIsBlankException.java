package com.siurbinfo.srs.exception;

// Lancada quando o nome do solicitante da reserva esta em branco.
public class NameRequesterIsBlankException extends RuntimeException {
    public NameRequesterIsBlankException(String message) {
        super(message);
    }
}
