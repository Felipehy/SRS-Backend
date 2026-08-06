package com.siurbinfo.srs.exception;

// Lancada quando o id de reserva informado nao existe na base.
public class ReservationIdIsNotFoundedException extends RuntimeException {
    public ReservationIdIsNotFoundedException(String message) {
        super(message);
    }
}
