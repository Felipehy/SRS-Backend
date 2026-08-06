package com.siurbinfo.srs.repository.reservation;

import com.siurbinfo.srs.enums.ReserveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository base generico (nao instanciado diretamente) com metodos comuns
 * as reservas de auditorio, sala de reuniao e veiculo.
 */
@NoRepositoryBean
public interface ReservationRepository<T> extends JpaRepository<T, Long>{

    // Contrato: busca o horario de termino da ultima reserva antes do horario informado (implementado por subtipo)
    Optional<LocalTime> findByReservationDateAndStartTime(LocalDate reservationDate, LocalTime startTime);
    // Contrato: retorna apenas o status da reserva pelo id (implementado por subtipo)
    ReserveStatus findStatusById(Long id);

}
