package com.siurbinfo.srs.exception;

// Lancada quando a acao solicitada nao pode ser feita pois a reserva ja foi aprovada.
public class ReservationWasApprove extends RuntimeException {
    public ReservationWasApprove(String message) {
        super(message);
    }
}
