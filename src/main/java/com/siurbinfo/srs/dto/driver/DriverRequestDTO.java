package com.siurbinfo.srs.dto.driver;

import jakarta.validation.constraints.NotBlank;

public record DriverRequestDTO(

        @NotBlank(message = "O nome do motorista não pode ser vazio")
        String name,

        String img
) {}
