package com.siurbinfo.srs.repository.user;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationsProjection(
        Long id,
        String reservationType,
        String status,
        LocalDate reservationDate,
        LocalTime startTime,
        LocalTime endTime,
        String userNameRequester,
        Long idUser,
        Long totalReservations
) {
}
