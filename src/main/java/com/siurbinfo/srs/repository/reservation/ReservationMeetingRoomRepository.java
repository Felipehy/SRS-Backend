package com.siurbinfo.srs.repository.reservation;

import com.siurbinfo.srs.entity.Reserve.MeetingRoomEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository de MeetingRoomEntity (reservas de sala de reuniao), com queries nativas
 * para verificacao de conflito de horario e aprovacao/rejeicao de reservas.
 */
public interface ReservationMeetingRoomRepository extends ReservationRepository<MeetingRoomEntity> {

    // Verifica se ja existe reserva conflitante para a sala/data/faixa de horario informados
    @Query(value = """
    select exists(
        select 1
        from tbl_meeting_room tmr
        where tmr.id_room=:roomId
        and tmr.reservation_date=:reservationDate
        and ((:startTime between tmr.start_time and tmr.end_time or :endTime between tmr.start_time and tmr.end_time) or (:endTime > tmr.end_time and :startTime < tmr.start_time))
    )
    """, nativeQuery = true)
    boolean existsReservationByIdAndDateAndHour(@Param("roomId") Long roomId,@Param("reservationDate") LocalDate reservationDate,@Param("startTime") LocalTime startTime,@Param("endTime") LocalTime endTime);

    // Lista as reservas de sala do mes/ano informado
    @Query(value = """
    select *
    from tbl_meeting_room tmr
    where extract(month from tmr.reservation_date) = :month
    and extract(year from tmr.reservation_date) = :year
    """, nativeQuery = true)
    List<MeetingRoomEntity> findByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    // Atualiza o status da reserva para aprovado
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_meeting_room
    set status=:status
    where id=:id
    """, nativeQuery = true)
    void approveReservationFromId(@Param("id") Long id, @Param("status") String status);

    // Busca o horario de termino da ultima reserva que encerra antes do horario informado, na mesma data
    @Override
    @Query(value = """
    select end_time
    from tbl_meeting_room tmr
    where tmr.reservation_date=:reservationDate
    and :startTime >= tmr.end_time
    order by tmr.end_time desc
    limit 1
    """,nativeQuery = true)
    Optional<LocalTime> findByReservationDateAndStartTime(@Param("reservationDate") LocalDate reservationDate, @Param("startTime") LocalTime startTime);

    // Atualiza o status da reserva para rejeitado e grava o motivo da rejeicao
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
    update tbl_meeting_room
    set status=:status, reason_failure=:reasonFailure
    where id=:id
    """, nativeQuery = true)
    void rejectReservationFromId(@Param("id") Long id, @Param("status") String status, @Param("reasonFailure") String reasonFailure);

    // Retorna apenas o status da reserva pelo id
    @Override
    @Query(value = """
    select status
    from tbl_meeting_room tmr
    where tmr.id=:id
    """, nativeQuery = true)
    ReserveStatus findStatusById(@Param("id") Long id);

}
