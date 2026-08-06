package com.siurbinfo.srs.dto.cognito.user;

public record UserResponse (
        String username,
        String given_name,
        String family_name,
        String email,
        String status
){}
