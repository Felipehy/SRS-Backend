package com.siurbinfo.srs.repository.reservation.projection;

import com.siurbinfo.srs.enums.ReserveStatus;

public interface StatusAndCoffeeProjection {

    ReserveStatus getStatus();
    Boolean getIsApproveCoffee();
    Boolean getIsCoffee();

}
