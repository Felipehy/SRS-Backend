package com.siurbinfo.srs.exception;

// Lancada quando o intervalo de tempo entre reservas nao respeita o minimo exigido.
public class InvalidTimeBetweenReservations extends RuntimeException {
    public InvalidTimeBetweenReservations(String message) {
        super(message);
    }
}
