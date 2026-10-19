package com.siurbinfo.srs.dto.glpi;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserNotifDTO(
        @JsonProperty("use_notification") Integer useNotification,
        @JsonProperty("alternative_email")  String  alternativeEmail
) {
}
