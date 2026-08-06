package com.siurbinfo.srs.dto.efs;

public record EfsResponseDTO (
        String fileName,
        Long fileLength,
        String contentType
){}
