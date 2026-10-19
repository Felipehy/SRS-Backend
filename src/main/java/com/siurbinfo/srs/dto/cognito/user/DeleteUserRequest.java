package com.siurbinfo.srs.dto.cognito.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record DeleteUserRequest(

        @Email
        @NotBlank
        String email
) {}
