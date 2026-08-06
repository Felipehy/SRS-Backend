package com.siurbinfo.srs.dto.cognito.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record UserGroupsRequest(

        @NotBlank
        String username,

        @NotEmpty
        String group
) {
}
