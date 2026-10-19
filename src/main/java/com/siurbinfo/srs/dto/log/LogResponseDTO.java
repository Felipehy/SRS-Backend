package com.siurbinfo.srs.dto.log;

import java.time.Instant;

public record LogResponseDTO (

    Long id,
    String reservationType,
    String modification,
    String localModification,
    String givenName,
    String familyName,
    String email,
    String ip,
    String groups,
    String typeLog,
    Instant modificationDate,
    Long idData

){
}