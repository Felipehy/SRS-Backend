package com.siurbinfo.srs.dto.reservation;

import com.siurbinfo.srs.dto.user.ListReservationsUserDTO;

import java.util.List;

public record ReservationsDTO(
        Long numReservations,
        List<ListReservationsUserDTO> reservations
) {
}
