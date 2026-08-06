package com.siurbinfo.srs.repository.reservation;

import com.siurbinfo.srs.entity.Reserve.FleetVehicleEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repository de FleetVehicleEntity (reservas de veiculos da frota), com queries nativas
 * para verificacao de conflito de horario e aprovacao/rejeicao de reservas.
 */
public interface ReservationFleetVehicleRepository extends JpaRepository<FleetVehicleEntity, Long> {

    // Verifica se ja existe reserva na mesma data com horario de saida posterior ao informado
    @Query(value = """
    select exists(
        select 1
        from tbl_fleet_vehicles tfv
        where tfv.reservation_date = :reservationDate
        and :exitTime > tfv.exit_time
    )
    """, nativeQuery = true)
    boolean existsByReservationDateAndHour(@Param("reservationDate") LocalDate reservationDate, @Param("exitTime") LocalTime exitTime);

    // Lista as reservas de veiculo do mes/ano informado
    @Query(value = " select * from tbl_fleet_vehicles tfv where extract(month from tfv.reservation_date) = :month and extract(year from tfv.reservation_date) = :year", nativeQuery = true)
    List<FleetVehicleEntity> findByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);

    // Atualiza o status da reserva para aprovado
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update tbl_fleet_vehicles set status=:status where id=:id", nativeQuery = true)
    void approveReservationFromId(@Param("id") Long id, @Param("status") String status);

    // Atualiza o status da reserva para rejeitado e grava o motivo da rejeicao
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update tbl_fleet_vehicles set status=:status, reason_failure=:reasonFailure where id=:id", nativeQuery = true)
    void rejectReservationFromId(@Param("id") Long id, @Param("status") String status, @Param("reasonFailure") String reasonFailure);

    // Retorna apenas o status da reserva pelo id
    @Query(value = """
    select status
    from tbl_fleet_vehicles tfv
    where tfv.id =:id
    """, nativeQuery = true)
    ReserveStatus findStatusById(@Param("id") Long id);

}
