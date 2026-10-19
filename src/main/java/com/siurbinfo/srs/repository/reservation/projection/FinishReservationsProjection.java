package com.siurbinfo.srs.repository.reservation.projection;

import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;

import java.time.LocalDate;

public interface FinishReservationsProjection{

    Long getId();
    LocalDate getReservationDate();
    ReserveStatus getStatus();
    ReserveType getReservationType();

}
