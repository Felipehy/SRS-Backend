package com.siurbinfo.srs.exception;

// Lancada quando ja existe outra reserva conflitante para a mesma sala/horario.
public class ReservationConflictException extends RuntimeException {
    public ReservationConflictException(String message) {
        super(message);
    }
}
