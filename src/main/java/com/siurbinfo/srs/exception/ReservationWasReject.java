package com.siurbinfo.srs.exception;

// Lancada quando a acao solicitada nao pode ser feita pois a reserva ja foi rejeitada.
public class ReservationWasReject extends RuntimeException {
    public ReservationWasReject(String message) {
        super(message);
    }
}
