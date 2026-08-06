package com.siurbinfo.srs.dto.driver;

import jakarta.validation.constraints.NotBlank;

// Dados de entrada para criar ou atualizar um motorista
public record DriverRequestDTO(

        // Nome é obrigatorio
        @NotBlank(message = "O nome do motorista não pode ser vazio")
        String name,

        String img
) {}
