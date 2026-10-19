package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomRequestDTO;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.exception.InvalidCoffeeException;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.repository.reservation.ReservationMeetingRoomRepository;
import com.siurbinfo.srs.repository.reservation.projection.StatusAndCoffeeProjection;
import com.siurbinfo.srs.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MeetingRoomValidator {

    private final ReservationValidator<MeetingRoomEntity> validator;
    private final ReservationMeetingRoomRepository repository;
    private final UserRepository userRepository;

    public void validateCreate(MeetingRoomRequestDTO dto, List<String> groups){

        validator.isStartTimeBeforeEndTime(dto.startTime(),dto.endTime());
        validator.isInvalidPeopleCount(dto.numPeople());
        validator.isAfterSevenAM(dto.startTime());
        validator.isOutsideBusinessHours(dto.endTime(), dto.reasonOutsideHours());
        if (validator.isSpecialUser(groups)) return;
        validator.isStartiTimeStartAfterATime(dto.reservationDate(),dto.startTime(),15,repository);
        if (repository.existsReservationByIdAndDateAndHour(dto.roomId(),dto.reservationDate(),dto.startTime(),dto.endTime(), ReserveStatus.APROVADA.name())){
            throw new ReservationConflictException("Já existe uma reserva nesta sala para esse dia");
        }
    }

    public void validateUpdate(MeetingRoomRequestDTO dto, MeetingRoomEntity entity){

        validator.isInvalidPeopleCount(dto.numPeople());
        validator.isStartTimeBeforeEndTime(dto.startTime(),dto.endTime());
        validator.isSameReservationDate(entity.getReservationDate(),dto.reservationDate());
        validator.isAfterSevenAM(dto.startTime());
        validator.isOutsideBusinessHours(dto.endTime(), dto.reasonOutsideHours());
        if (repository.existsReservationByIdAndDateAndHour(dto.roomId(),dto.reservationDate(),dto.startTime(),dto.endTime(), ReserveStatus.APROVADA.name())){
            throw new ReservationConflictException("Já existe uma reserva nesta sala para esse dia");
        }
    }

    public void validatorApprove(Long id){
        StatusAndCoffeeProjection status = repository.findStatusAndStatusCoffeeById(id);
        validator.isPossibleApproveOrReject(status);
    }

    public void validatorApproveCoffee(Long id){
        StatusAndCoffeeProjection isApproveCoffee = repository.findStatusAndStatusCoffeeById(id);
        if (isApproveCoffee.getStatus() == ReserveStatus.CANCELADA || isApproveCoffee.getStatus() == ReserveStatus.FINALIZADA) {
            throw new InvalidCoffeeException("A reserva foi cancelada ou está finalizada");
        }
        if (isApproveCoffee.getIsApproveCoffee() == null) return;
        if (isApproveCoffee.getIsApproveCoffee()) throw new InvalidCoffeeException("Já foi aprovado o coffee break");
    }

    public void validatorReject(Long id){
        StatusAndCoffeeProjection status = repository.findStatusAndStatusCoffeeById(id);
        validator.isPossibleApproveOrReject(status);
    }

    public void validatorRejectCoffee(Long id){
        StatusAndCoffeeProjection isApproveCoffee = repository.findStatusAndStatusCoffeeById(id);
        if (isApproveCoffee.getStatus() == ReserveStatus.CANCELADA || isApproveCoffee.getStatus() == ReserveStatus.FINALIZADA) {
            throw new InvalidCoffeeException("A reserva foi cancelada ou está finalizada");
        }
        if (isApproveCoffee.getIsApproveCoffee() == null) return;
        if (!isApproveCoffee.getIsApproveCoffee()) throw new InvalidCoffeeException("Já foi reprovado o coffee break");
    }

    public void validatorCancel(Long id,String email, List<String> groups){
        MeetingRoomEntity entity = repository.findById(id).orElseThrow(() -> new ReservationIdIsNotFoundedException("Não foi encontrado a reserva"));
        Long idUser = userRepository.getIdByEmail(email);
        validator.isPossibleCanceled(entity.getStatus(),idUser,entity.getUser().getId(),groups);
    }

}
