package com.siurbinfo.srs.repository.reservation;

import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservationFleetVehicleRepository extends IreservationRepository<FleetVehicleEntity> {

    @Query(value = """
    select exists(
        select 1
        from tbl_fleet_vehicles tfv
        where tfv.reservation_date = :reservationDate
        and :exitTime > tfv.exit_time
    )
    """, nativeQuery = true)
    boolean existsByReservationDateAndHour(@Param("reservationDate") LocalDate reservationDate, @Param("exitTime") LocalTime exitTime);

    @Query(value = " select * from tbl_fleet_vehicles tfv where extract(month from tfv.reservation_date) = :month and extract(year from tfv.reservation_date) = :year", nativeQuery = true)
    List<FleetVehicleEntity> findByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    @Query(value = """
    select status
    from tbl_fleet_vehicles tfv
    where tfv.id =:id
    """, nativeQuery = true)
    ReserveStatus findStatusById(@Param("id") Long id);

    @Override
    @Query(value = """
        select exit_time
        from tbl_fleet_vehicles tfv
        where tfv.reservation_date = :reservationDate
        and tfv.status = :status
        and tfv.exit_time <= :exitTime
        order by tfv.exit_time desc
        limit 1
    """,nativeQuery = true)
    Optional<LocalTime> findByReservationDateAndStartTime(@Param("reservationDate") LocalDate reservationDate,@Param("exitTime") LocalTime exitTime,@Param("status") String status);

    @Query(value = """
        select reservation_date
        from tbl_fleet_vehicles tfv
        where tfv.reservation_date = :reservationDate
        and tfv.status = :status
        order by reservation_date desc
        limit 1
    """,nativeQuery = true)
    Optional<LocalDate> findReservationByDate(@Param("reservationDate") LocalDate reservationDate, @Param("status") String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update tbl_fleet_vehicles set status=:status, user_name_approver=:name, date_analysis=:date where id=:id and status = 'ENVIADA_PARA_ANALISE'", nativeQuery = true)
    void approveReservationFromId(@Param("id") Long id, @Param("status") String status, @Param("name") String name, @Param("date") Instant date);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update tbl_fleet_vehicles set status=:status, reason_failure=:reasonFailure, user_name_approver=:name, date_analysis=:date where id=:id and status = 'ENVIADA_PARA_ANALISE'", nativeQuery = true)
    void rejectReservationFromId(@Param("id") Long id, @Param("status") String status, @Param("reasonFailure") String reasonFailure, @Param("name") String name, @Param("date") Instant date);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_fleet_vehicles
    set status = 'CANCELADA', reasons_cancellation = :reasonCancellation, date_cancellation = :dateCancellation, canceled_by = :idUser
    where id = :id
    """, nativeQuery = true)
    void cancelReservation(@Param("id") Long id, @Param("idUser") Long idUser, @Param("reasonCancellation") String reasonCancellation, @Param("dateCancellation") Instant dateCancellation);

}
