package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.config.TimeZoneConfig;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.exception.*;
import com.siurbinfo.srs.repository.reservation.IreservationRepository;
import com.siurbinfo.srs.repository.reservation.projection.StatusAndCoffeeProjection;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Component
public class ReservationValidator<T> {

    void isStartTimeBeforeEndTime(LocalTime start, LocalTime end){
        if (!start.isBefore(end)) throw new StartTimeIsNotBeforeEndTimeException("O tempo inicial é maior ou igual que o tempo final");
    }

    void isInvalidPeopleCount(Integer numPeople){
        if (numPeople <= 0) throw new InvalidPeopleCountException("É necessário pelo menos 1 pessoa para reservar");
    }

    // Exige um intervalo minimo entre esta reserva e a anterior APROVADA do mesmo dia.
    void isStartiTimeStartAfterATime(LocalDate reservationDate, LocalTime start, Integer time, IreservationRepository<T> repository){
        Optional<LocalTime> reservation = repository.findByReservationDateAndStartTime(reservationDate,start, ReserveStatus.APROVADA.name());
        if (reservation.isPresent() && ChronoUnit.MINUTES.between(reservation.get(),start) < time) {
            String textTime = time >= 60 ? String.format("%s horas",time/60) : String.format("%s minutos", time);
            throw new InvalidTimeBetweenReservations(String.format("O tempo entre as reservas tem que ser de %s",textTime));
        }
    }

    // Barra reserva feita com mais de maxDays de antecedencia. "Hoje" e o dia de Sao Paulo,
    // nao o da JVM: o container roda em UTC e depois das 21h ja estaria no dia seguinte.
    public void validateIntervalDays(LocalDate reservationDate, Integer maxDays){
        long days = ChronoUnit.DAYS.between(LocalDate.now(TimeZoneConfig.ZONE),reservationDate);
        if (days > maxDays) throw new InvalidReservationException(String.format("Tem que ter no máximo %s dias de antecipação",maxDays));
    }

    // No update, a data precisa mudar: manter o mesmo dia e tratado como conflito.
    void isSameReservationDate(LocalDate entity, LocalDate dto){
        if (entity.isEqual(dto)) throw new ReservationConflictException("Já existe uma reserva para este dia");
    }

    void isPossibleApproveOrReject(StatusAndCoffeeProjection status){
        if (status.getStatus() == ReserveStatus.CANCELADA) throw new ReservationWasCancell("A reserva foi cancelada");
        if (status.getStatus() == ReserveStatus.APROVADA) throw new ReservationWasApprove("A reserva já foi aprovada");
        if (status.getStatus() == ReserveStatus.FINALIZADA) throw new ReservationWasFinshed("A reserva já terminou");
        if (status.getStatus() == ReserveStatus.REPROVADA) throw new ReservationWasReject("A reserva já foi reprovada");
        if (status.getIsApproveCoffee() == null && Boolean.TRUE.equals(status.getIsCoffee())) throw new ReservationConflictException("É necessário aprovar ou rejeitar o café!");
    }

    void isPossibleApproveOrReject(ReserveStatus status){
        if (status == ReserveStatus.CANCELADA) throw new ReservationWasCancell("A reserva foi cancelada");
        if (status == ReserveStatus.APROVADA) throw new ReservationWasApprove("A reserva já foi aprovada");
        if (status == ReserveStatus.FINALIZADA) throw new ReservationWasFinshed("A reserva já terminou");
        if (status == ReserveStatus.REPROVADA) throw new ReservationWasReject("A reserva já foi reprovada");
    }

    public void isPossibleCanceled(ReserveStatus status, Long idUser, Long idUserInRepository, List<String> groups){
        if (groups == null) {throw new SecurityException("the user must be in a group");}
        if (status.equals(ReserveStatus.CANCELADA)){
            throw new ReservationWasCancell("A reserva já foi cancelada");
        }
        if (!idUser.equals(idUserInRepository)){
            for (String group: groups){
                if (group.equalsIgnoreCase("siurb-administrador") || group.equalsIgnoreCase("siurb-divisao-administrativa")){
                    return;
                }
            }
            throw new SecurityException("Você não é o solicitante dessa reserva");
        }
    }

    public void isAfterSevenAM(LocalTime hours){
        if (LocalTime.of(7,0,0).isAfter(hours)) {
            throw new InvalidTimeException("O horário precisa ser depois das 7 da manhã");
        }
    }

    public void isOutsideBusinessHours(LocalTime hours, String reasonOutsideHours){
        boolean isBlankReason = reasonOutsideHours == null || reasonOutsideHours.isBlank();
        if (hours.isAfter(LocalTime.of(19,0,0)) && isBlankReason) {
            throw new InvalidTimeException("É necessário o motivo para reservas depois das 19h");
        }
    }

    public boolean isSpecialUser(List<String> groups){
        for (String group: groups){
            if (group.equalsIgnoreCase("siurb-usuario-geral-especial")){
                return true;
            }
        }
        return false;
    }
}
