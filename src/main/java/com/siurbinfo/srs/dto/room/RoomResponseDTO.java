package com.siurbinfo.srs.dto.room;

import lombok.Builder;

@Builder
public record RoomResponseDTO(
        Long id,
        String floor,
        Boolean isActive
) {}
