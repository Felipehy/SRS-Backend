package com.siurbinfo.srs.repository.log;

import java.time.Instant;

public interface LogProjection {

    Long getId();
    String getReservationType();
    String getModification();
    String getLocalModification();
    String getGivenName();
    String getFamilyName();
    String getEmail();
    String getIp();
    String getGroupUser();
    String getTypeLog();
    Instant getModificationDate();
    Long getIdData();


}
