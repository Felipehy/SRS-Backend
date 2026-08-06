package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.entity.Reserve.AuditoriumEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.repository.reservation.ReservationAuditoriumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Validador de regras de negocio para reservas de auditorio: horario valido,
// quantidade de pessoas, conflito de horario e intervalo minimo entre reservas.
@Component
public class AuditoriumValidator {

    @Autowired
    ReservationValidator<AuditoriumEntity> validator;

    @Autowired
    ReservationAuditoriumRepository repository;

    // Valida a criacao de uma reserva de auditorio.
    public void validatorCreate(AuditoriumRequestDTO dto){
        validator.isStartTimeBeforeEndTime(dto.startTime(),dto.endTime());
        validator.isInvalidPeopleCount(dto.numPeople());
        // conflito: ja existe reserva do auditorio nesse mesmo dia/horario
        if (repository.existsByReservationDateAndHour(dto.reservationDate(),dto.startTime(),dto.endTime())){
            throw new ReservationConflictException("O auditório já está reservado neste horário. Escolha outro horário ou sala");
        }
        // exige intervalo minimo de 30 minutos entre esta reserva e a anterior no mesmo dia
        validator.isStartiTimeStartAfterATime(dto.reservationDate(),dto.startTime(),30,repository);
    }

    // Valida a atualizacao de uma reserva de auditorio existente.
    public void validatorUpdate(AuditoriumRequestDTO dto, AuditoriumEntity entity){
        validator.isInvalidPeopleCount(dto.numPeople());
        validator.isStartTimeBeforeEndTime(dto.startTime(),dto.endTime());
        validator.isSameReservationDate(entity.getReservationDate(),dto.reservationDate());
        // conflito: ja existe reserva do auditorio nesse mesmo dia/horario
        if (repository.existsByReservationDateAndHour(dto.reservationDate(),dto.startTime(),dto.endTime())){throw new ReservationConflictException("Já existe uma reserva nesta sala para esse dia");}
    }

    // Valida se a reserva pode ser aprovada de acordo com o status atual.
    public void validatorApprove(Long id){
        ReserveStatus status = repository.findStatusById(id);
        validator.isPossibleApprove(status);
    }

    // Valida se a reserva pode ser reprovada de acordo com o status atual.
    public void validatorReject(Long id){
        ReserveStatus status = repository.findStatusById(id);
        validator.isPossibleReject(status);
    }

}
