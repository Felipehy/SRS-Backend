package com.siurbinfo.srs.dto.cognito.group;

import jakarta.validation.constraints.NotBlank;

public record GroupRequest(

        @NotBlank
        String groupName,
        @NotBlank
        String description
) {}
