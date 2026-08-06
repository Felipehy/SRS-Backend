package com.siurbinfo.srs.repository.reservation;

import com.siurbinfo.srs.entity.Reserve.AuditoriumEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository de AuditoriumEntity (reservas de auditorio), com queries nativas
 * para verificacao de conflito de horario e aprovacao/rejeicao de reservas.
 */
public interface ReservationAuditoriumRepository extends ReservationRepository<AuditoriumEntity> {

    // Verifica se ja existe reserva conflitante na data/faixa de horario informada
    @Query(value = """
    select exists(
        select 1
        from tbl_auditorium ta
        where ta.reservation_date = :reservationDate
        and ((:startTime between ta.start_time and ta.end_time or :endTime between ta.start_time and ta.end_time) or (:endTime > ta.end_time and :startTime < ta.start_time))
    )
    """, nativeQuery = true)
    boolean existsByReservationDateAndHour(@Param("reservationDate") LocalDate reservationDate, @Param("startTime") LocalTime startTime,@Param("endTime") LocalTime endTime);

    // Busca o horario de termino da ultima reserva que encerra antes do horario informado, na mesma data
    @Override
    @Query(value = """
    select end_time
    from tbl_auditorium ta
    where ta.reservation_date=:reservationDate
    and :startTime >= ta.end_time
    order by ta.end_time desc
    limit 1
    """,nativeQuery = true)
    Optional<LocalTime> findByReservationDateAndStartTime(@Param("reservationDate") LocalDate reservationDate, @Param("startTime") LocalTime startTime);


    // Lista as reservas de auditorio do mes/ano informado
    @Query(value = "select * from tbl_auditorium ta where extract(month from ta.reservation_date) = :month and extract(year from ta.reservation_date) = :year", nativeQuery = true)
    List<AuditoriumEntity> findByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    // Atualiza o status da reserva para aprovado
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update tbl_auditorium set status=:status where id=:id", nativeQuery = true)
    void approveReservationFromId(@Param("id") Long id, @Param("status") String status);

    // Atualiza o status da reserva para rejeitado e grava o motivo da rejeicao
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update tbl_auditorium set status=:status, reason_failure=:reasonFailure where id=:id", nativeQuery = true)
    void rejectReservationFromId(@Param("id") Long id, @Param("status") String status, @Param("reasonFailure") String reasonFailure);

    // Retorna apenas o status da reserva pelo id
    @Override
    @Query(value = """
    select status
    from tbl_auditorium ta
    where ta.id=:id
    """,nativeQuery = true)
    ReserveStatus findStatusById(@Param("id") Long id);

}
