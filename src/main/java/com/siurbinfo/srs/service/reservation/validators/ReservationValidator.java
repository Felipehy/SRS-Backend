package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.exception.*;
import com.siurbinfo.srs.repository.reservation.ReservationRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

// Validador generico com regras comuns a todos os tipos de reserva (auditorio, sala,
// veiculo): horario, quantidade de pessoas, intervalo entre reservas e transicao de status.
// Reutilizado pelos validators especificos de cada tipo de reserva.
@Component
public class ReservationValidator<T> {

    // Garante que o horario inicial seja anterior ao horario final.
    void isStartTimeBeforeEndTime(LocalTime start, LocalTime end){
        if (!start.isBefore(end)){throw new StartTimeIsNotBeforeEndTimeException("O tempo inicial é maior ou igual que o tempo final");}
    }

    // Garante que a quantidade de pessoas seja maior que zero.
    void isInvalidPeopleCount(Integer numPeople){
        if (numPeople <= 0){throw new InvalidPeopleCountException("É necessário pelo menos 1 pessoa para reservar");}
    }

    // Garante um intervalo minimo (em minutos) entre o inicio desta reserva e o inicio
    // da reserva anterior encontrada no mesmo dia.
    void isStartiTimeStartAfterATime(LocalDate reservationDate, LocalTime start, Integer time, ReservationRepository<T> repository){
        Optional<LocalTime> reservation = repository.findByReservationDateAndStartTime(reservationDate,start);
        if (reservation.isPresent() && ChronoUnit.MINUTES.between(reservation.get(),start) < time) {
            throw new InvalidTimeBetweenReservations(String.format("O tempo entre as reservas tem que ser de %s minutos",time));
        }
    }

    // Impede que a data da reserva seja alterada para o mesmo dia que ja estava definido.
    void isSameReservationDate(LocalDate entity, LocalDate dto){
        if (entity.isEqual(dto)){throw new ReservationConflictException("Já existe uma reserva para este dia");}
    }

    // Verifica se o status atual permite aprovar a reserva (bloqueia se ja cancelada,
    // aprovada, finalizada ou reprovada).
    void isPossibleApprove(ReserveStatus status){
        if (status == ReserveStatus.CANCELADA) {throw new ReservationWasCancell("A reserva foi cancelada");}
        if (status == ReserveStatus.APROVADA) {throw new ReservationWasApprove("A reserva já foi aprovada");}
        if (status == ReserveStatus.FINALIZADA) {throw new ReservationWasFinshed("A reserva já terminou");}
        if (status == ReserveStatus.REPROVADA) {throw new ReservationWasReject("A reserva foi cancelada");}
    }

    // Verifica se o status atual permite reprovar a reserva (bloqueia se ja cancelada,
    // aprovada, finalizada ou reprovada).
    void isPossibleReject(ReserveStatus status){
        if (status == ReserveStatus.CANCELADA) {throw new ReservationWasCancell("A reserva foi cancelada");}
        if (status == ReserveStatus.APROVADA) {throw new ReservationWasApprove("A reserva foi aprovada");}
        if (status == ReserveStatus.FINALIZADA) {throw new ReservationWasFinshed("A reserva já terminou");}
        if (status == ReserveStatus.REPROVADA) {throw new ReservationWasReject("A reserva já foi cancelada");}
    }

}
