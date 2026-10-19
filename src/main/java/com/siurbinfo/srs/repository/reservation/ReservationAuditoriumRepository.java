package com.siurbinfo.srs.repository.reservation;

import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.repository.reservation.projection.StatusAndCoffeeProjection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservationAuditoriumRepository extends IreservationRepository<AuditoriumEntity> {

    // true se a faixa pedida encosta em alguma reserva ja existente no mesmo dia
    @Query(value = """
    select exists(
        select 1
        from tbl_auditorium ta
        where ta.reservation_date = :reservationDate
        and status =:status
        and ((:startTime between ta.start_time and ta.end_time or :endTime between ta.start_time and ta.end_time) or (:endTime > ta.end_time and :startTime < ta.start_time))
    )
    """, nativeQuery = true)
    boolean existsByReservationDateAndHour(@Param("reservationDate") LocalDate reservationDate, @Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime, @Param("status") String status);

    // end_time da ultima reserva que termina antes do horario pedido, no mesmo dia
    @Override
    @Query(value = """
    select end_time
    from tbl_auditorium ta
    where ta.reservation_date=:reservationDate
    and :startTime >= ta.end_time
    and status =:status
    order by ta.end_time desc
    limit 1
    """,nativeQuery = true)
    Optional<LocalTime> findByReservationDateAndStartTime(@Param("reservationDate") LocalDate reservationDate, @Param("startTime") LocalTime startTime, @Param("status") String status);

    @Query(value = "select * from tbl_auditorium ta where extract(month from ta.reservation_date) = :month and extract(year from ta.reservation_date) = :year", nativeQuery = true)
    List<AuditoriumEntity> findByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update tbl_auditorium
        set status=:status, user_name_approver=:name, date_analysis=:date
        where id=:id and status = 'ENVIADA_PARA_ANALISE'
        """, nativeQuery = true)
    void approveReservationFromId(@Param("id") Long id, @Param("status") String status, @Param("name") String name, @Param("date") Instant date);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        update tbl_auditorium
        set status=:status, reason_failure=:reasonFailure, user_name_approver=:name, date_analysis=:date
        where id=:id and status = 'ENVIADA_PARA_ANALISE'
        """, nativeQuery = true)
    void rejectReservationFromId(@Param("id") Long id, @Param("status") String status, @Param("reasonFailure") String reasonFailure, @Param("name") String name, @Param("date") Instant date);

    @Query(value = """
    select status as status,
            is_approve_coffee_break as isApproveCoffee,
            is_necessary_coffee as isCoffee
    from tbl_auditorium ta
    where ta.id=:id
    """,nativeQuery = true)
    StatusAndCoffeeProjection findStatusAndStatusCoffeeById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_auditorium
    set is_approve_coffee_break=false, reason_reject_coffee_break =:reasonRejectCoffee
    where id=:id and (is_approve_coffee is null or is_approve_coffee = true)
    """, nativeQuery = true)
    void rejectCoffeeFromId(@Param("id") Long id,@Param("reasonRejectCoffee") String reasonRejectCoffee);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_auditorium
    set is_approve_coffee_break=true, reason_reject_coffee_break = null
    where id=:id and (is_approve_coffee is null or is_approve_coffee = false)
    """, nativeQuery = true)
    void approveCoffeeFromId(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_auditorium
    set id_ticket = :idTicket
    where id = :id
    """, nativeQuery = true)
    void putIdTicket(@Param("idTicket") Long idTicket, @Param("id") Long id);

    @Query(value = """
    select status
    from tbl_auditorium
    where id = :id
    """, nativeQuery = true)
    ReserveStatus getStatusReservationById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_auditorium
    set status = 'CANCELADA', reasons_cancellation = :reasonCancellation, date_cancellation = :dateCancellation, canceled_by = :idUser
    where id = :id
    """, nativeQuery = true)
    void cancelReservation(@Param("id") Long id, @Param("idUser") Long idUser, @Param("reasonCancellation") String reasonCancellation, @Param("dateCancellation") Instant dateCancellation);

}
