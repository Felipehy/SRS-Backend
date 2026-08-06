package com.siurbinfo.srs.exception;

// Lancada quando a acao solicitada nao pode ser feita pois a reserva ja foi cancelada.
public class ReservationWasCancell extends RuntimeException {
    public ReservationWasCancell(String message) {
        super(message);
    }
}
