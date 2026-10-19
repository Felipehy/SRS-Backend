package com.siurbinfo.srs.dto.user;

import java.time.LocalDate;
import java.time.LocalTime;

public record ListReservationsUserDTO(
        Long id,
        String reservationType,
        String status,
        LocalDate reservationDate,
        LocalTime startTime,
        LocalTime endTime,
        String userNameRequester,
        Long idUser
) {
}
