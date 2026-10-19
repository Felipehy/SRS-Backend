package com.siurbinfo.srs.dto.driver;

import lombok.Builder;

@Builder
public record DriverResponseDTO(
   Long id,
   String name,
   String img
) {}
