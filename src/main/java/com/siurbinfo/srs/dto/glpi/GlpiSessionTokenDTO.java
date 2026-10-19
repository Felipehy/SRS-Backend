package com.siurbinfo.srs.dto.glpi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record GlpiSessionTokenDTO(
        @JsonProperty("session_token") String sessionToken,
        Integer expireToken,
        Instant createToken
) {
}
