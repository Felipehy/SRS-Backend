package com.siurbinfo.srs.repository.reservation;

import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.repository.reservation.projection.StatusAndCoffeeProjection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ReservationMeetingRoomRepository extends IreservationRepository<MeetingRoomEntity> {

    // true se a faixa pedida encosta em alguma reserva ja existente na mesma sala e dia
    @Query(value = """
    select exists(
        select 1
        from tbl_meeting_room tmr
        where tmr.id_room=:roomId
        and tmr.reservation_date=:reservationDate
        and status =:status
        and ((:startTime between tmr.start_time and tmr.end_time or :endTime between tmr.start_time and tmr.end_time) or (:endTime > tmr.end_time and :startTime < tmr.start_time))
    )
    """, nativeQuery = true)
    boolean existsReservationByIdAndDateAndHour(@Param("roomId") Long roomId,@Param("reservationDate") LocalDate reservationDate,@Param("startTime") LocalTime startTime,@Param("endTime") LocalTime endTime, @Param("status") String status);

    @Query(value = """
    select *
    from tbl_meeting_room tmr
    where extract(month from tmr.reservation_date) = :month
    and extract(year from tmr.reservation_date) = :year
    """, nativeQuery = true)
    List<MeetingRoomEntity> findByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_meeting_room
    set status=:status, user_name_approver=:name, date_analysis=:date
    where id=:id and status = 'ENVIADA_PARA_ANALISE'
    """, nativeQuery = true)
    void approveReservationFromId(@Param("id") Long id, @Param("status") String status, @Param("name") String name, @Param("date") Instant date);

    // end_time da ultima reserva que termina antes do horario pedido, no mesmo dia
    @Override
    @Query(value = """
    select end_time
    from tbl_meeting_room tmr
    where tmr.reservation_date=:reservationDate
    and :startTime >= tmr.end_time
    and status =:status
    order by tmr.end_time desc
    limit 1
    """,nativeQuery = true)
    Optional<LocalTime> findByReservationDateAndStartTime(@Param("reservationDate") LocalDate reservationDate, @Param("startTime") LocalTime startTime, @Param("status") String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_meeting_room
    set status=:status, reason_failure=:reasonFailure, user_name_approver=:name, date_analysis=:date
    where id=:id and status = 'ENVIADA_PARA_ANALISE'
    """, nativeQuery = true)
    void rejectReservationFromId(@Param("id") Long id, @Param("status") String status, @Param("reasonFailure") String reasonFailure, @Param("name") String name, @Param("date") Instant date);


    @Query(value = """
    select status as status,
            is_approve_coffee as isApproveCoffee,
            is_coffee as isCoffee
    from tbl_meeting_room tmr
    where tmr.id=:id
    """, nativeQuery = true)
    StatusAndCoffeeProjection findStatusAndStatusCoffeeById(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_meeting_room
    set is_approve_coffee=false, reason_reject_coffee =:reasonRejectCoffee
    where id=:id and (is_approve_coffee is null or is_approve_coffee = true)
    """, nativeQuery = true)
    void rejectCoffeeFromId(@Param("id") Long id,@Param("reasonRejectCoffee") String reasonRejectCoffee);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_meeting_room
    set is_approve_coffee=true, reason_reject_coffee = null
    where id=:id and (is_approve_coffee is null or is_approve_coffee = false)
    """, nativeQuery = true)
    void approveCoffeeFromId(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_meeting_room
    set status = 'CANCELADA', reasons_cancellation = :reasonCancellation, date_cancellation = :dateCancellation, canceled_by = :idUser
    where id = :id
    """, nativeQuery = true)
    void cancelReservation(@Param("id") Long id, @Param("idUser") Long idUser, @Param("reasonCancellation") String reasonCancellation, @Param("dateCancellation") Instant dateCancellation);

}
