package com.siurbinfo.srs.exception;

/**
 * Falha ao consultar o Cognito (indisponibilidade, throttling, rede etc.).
 * Representa erro de dependencia externa, nao erro do cliente.
 */
public class CognitoQueryException extends RuntimeException {
    public CognitoQueryException(String message) {
        super(message);
    }
}
