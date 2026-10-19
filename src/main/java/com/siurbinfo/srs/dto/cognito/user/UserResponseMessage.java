package com.siurbinfo.srs.dto.cognito.user;

public record UserResponseMessage(
        Long id,
        String message,
        String given_name
){}
