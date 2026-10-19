package com.siurbinfo.srs.dto.reservation.reason;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCoffeeDTO(

        @Size(message = "O maximo de caracteres permitidos são 300", max = 300)
        @NotBlank(message = "Não pode estar vazio o campo")
        String reasonRejectCoffee

) {
}
