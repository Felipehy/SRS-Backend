package com.siurbinfo.srs.exception;

// Lancada quando a acao solicitada nao pode ser feita pois a reserva ja foi finalizada.
public class ReservationWasFinshed extends RuntimeException {
    public ReservationWasFinshed(String message) {
        super(message);
    }
}
