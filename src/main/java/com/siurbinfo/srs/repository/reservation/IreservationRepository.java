package com.siurbinfo.srs.repository.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@NoRepositoryBean
public interface IreservationRepository<T> extends JpaRepository<T, Long>{

    // status vai como String porque em query nativa o Hibernate ignora o @Enumerated e manda o enum pelo ordinal, que nao casa com o varchar(30).
    Optional<LocalTime> findByReservationDateAndStartTime(LocalDate reservationDate, LocalTime startTime, String status);

}
