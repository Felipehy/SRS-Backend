package com.siurbinfo.srs.exception;

// Lancada quando o endereco de partida e igual ao endereco de destino.
public class SameDepartureAndDestinationException extends RuntimeException {
    public SameDepartureAndDestinationException(String message) {
        super(message);
    }
}
