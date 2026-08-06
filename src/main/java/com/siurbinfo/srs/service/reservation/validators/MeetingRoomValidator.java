package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomRequestDTO;
import com.siurbinfo.srs.entity.Reserve.MeetingRoomEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.repository.reservation.ReservationMeetingRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Validador de regras de negocio para reservas de sala de reuniao: horario valido,
// quantidade de pessoas, conflito de horario na mesma sala e intervalo minimo entre reservas.
@Component
@RequiredArgsConstructor
public class MeetingRoomValidator {

    private final ReservationValidator<MeetingRoomEntity> validator;
    private final ReservationMeetingRoomRepository repository;

    // Valida a criacao de uma reserva de sala de reuniao.
    public void validateCreate(MeetingRoomRequestDTO dto){
        validator.isStartTimeBeforeEndTime(dto.startTime(),dto.endTime());
        validator.isInvalidPeopleCount(dto.numPeople());
        // conflito: ja existe reserva para essa sala nesse mesmo dia/horario
        if (repository.existsReservationByIdAndDateAndHour(dto.roomId(),dto.reservationDate(),dto.startTime(),dto.endTime())){
            throw new ReservationConflictException("Já existe uma reserva nesta sala para esse dia");
        }
        // exige intervalo minimo de 15 minutos entre esta reserva e a anterior no mesmo dia
        validator.isStartiTimeStartAfterATime(dto.reservationDate(),dto.startTime(),15,repository);
    }

    // Valida a atualizacao de uma reserva de sala de reuniao existente.
    public void validateUpdate(MeetingRoomRequestDTO dto, MeetingRoomEntity entity){
        validator.isInvalidPeopleCount(dto.numPeople());
        validator.isStartTimeBeforeEndTime(dto.startTime(),dto.endTime());
        validator.isSameReservationDate(entity.getReservationDate(),dto.reservationDate());
        // conflito: ja existe reserva para essa sala nesse mesmo dia/horario
        if (repository.existsReservationByIdAndDateAndHour(dto.roomId(),dto.reservationDate(),dto.startTime(),dto.endTime())){throw new ReservationConflictException("Já existe uma reserva nesta sala para esse dia");}
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
