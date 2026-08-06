package com.siurbinfo.srs.dto.cognito.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UserRequest(

        @NotBlank(message = "É necessário o primeiro nome")
        String givenName,

        @NotBlank(message = "É necessário o sobrenome")
        String familyName,

        @Email(message = "Email inválido")
        @NotBlank(message = "É necessário o email")
        String email,

        // Opcional. Aceita apenas dígitos, com "+55" opcional (ex.: 11999998888 ou +5511999998888).
        // A normalização para o formato E.164 exigido pelo Cognito é feita no service.
        @Pattern(
                regexp = "^$|^\\+?\\d{10,13}$",
                message = "Número de telefone inválido. Use DDD + número (ex.: 11999998888)"
        )
        String phoneNumber

) {}
