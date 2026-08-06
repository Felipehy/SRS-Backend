package com.siurbinfo.srs.exception;

// Lancada quando a requisicao ou algum campo obrigatorio chega vazio.
public class EmptyRequestException extends RuntimeException {
    public EmptyRequestException(String message) {
        super(message);
    }
}
