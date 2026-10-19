package com.siurbinfo.srs.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record DepartmentRequestDTO (

        @NotBlank(message = "Não pode estar vazio")
        @Length(max = 50, message = "Ultrapassou o limite de caracteres")
        String name,

        @NotNull(message = "Não pode estar vazio")
        Long idRoom

){}
