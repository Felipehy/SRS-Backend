package com.siurbinfo.srs.exception;

// Lancada quando o horario de inicio da reserva nao e anterior ao horario de termino.
public class StartTimeIsNotBeforeEndTimeException extends RuntimeException {
    public StartTimeIsNotBeforeEndTimeException(String message) {
        super(message);
    }
}
