package com.siurbinfo.srs.exception;

/**
 * Token invalido ou sem o escopo "aws.cognito.signin.user.admin" exigido pelo
 * GetUser. Sinaliza problema de autorizacao do proprio token do usuario.
 */
public class CognitoUnauthorizedException extends RuntimeException {
    public CognitoUnauthorizedException(String message) {
        super(message);
    }
}
