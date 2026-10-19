package com.siurbinfo.srs.dto.reservation.auditorium;

import java.time.LocalTime;

public record CoffeeBreakDTO(
        Boolean necessaryCoffee,
        String reasonCoffee,
        LocalTime foodSetupTime,
        LocalTime serviceTime,
        Boolean isApproveCoffeeBreak,
        String reasonRejectCoffee
) {
}
