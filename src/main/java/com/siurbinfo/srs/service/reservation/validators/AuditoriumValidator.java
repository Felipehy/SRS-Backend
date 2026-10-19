package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.exception.InvalidCoffeeException;
import com.siurbinfo.srs.exception.InvalidTimeException;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.repository.reservation.ReservationAuditoriumRepository;
import com.siurbinfo.srs.repository.reservation.projection.StatusAndCoffeeProjection;
import com.siurbinfo.srs.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuditoriumValidator {

    private final ReservationValidator<AuditoriumEntity> validator;
    private final ReservationAuditoriumRepository repository;
    private final UserRepository userRepository;

    public void validatorCreate(AuditoriumRequestDTO dto, List<String> groups){

        validator.isStartTimeBeforeEndTime(dto.startTime(),dto.endTime());
        validator.isInvalidPeopleCount(dto.numPeople());
        validator.isAfterSevenAM(dto.timeOpenAuditorium());
        validator.isAfterSevenAM(dto.startTime());
        validator.isOutsideBusinessHours(dto.endTime(), dto.reasonOutsideHours());
        validateCoffeeBreakTime(dto);
        //Usuarios especiais podem reservar a qualquer horario mesmo estando aprovado
        if (validator.isSpecialUser(groups)) return;
        validator.isStartiTimeStartAfterATime(dto.reservationDate(),dto.startTime(),30,repository);
        if (repository.existsByReservationDateAndHour(dto.reservationDate(),dto.startTime(),dto.endTime(),ReserveStatus.APROVADA.name())){
            throw new ReservationConflictException("O auditório possui uma reserva aprovada neste horário. Escolha outro horário ou sala");
        }
    }

    public void validatorUpdate(AuditoriumRequestDTO dto, AuditoriumEntity entity){

        validator.isInvalidPeopleCount(dto.numPeople());
        validator.isStartTimeBeforeEndTime(dto.startTime(),dto.endTime());
        validator.isAfterSevenAM(dto.timeOpenAuditorium());
        validator.isAfterSevenAM(dto.startTime());
        validator.isOutsideBusinessHours(dto.endTime(), dto.reasonOutsideHours());
        validateCoffeeBreakTime(dto);
        if (repository.existsByReservationDateAndHour(dto.reservationDate(),dto.startTime(),dto.endTime(),ReserveStatus.APROVADA.name())){
            throw new ReservationConflictException("O auditório possui uma reserva aprovada neste horário. Escolha outro horário ou sala");
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
        if(isApproveCoffee.getIsApproveCoffee()) {
            throw new InvalidCoffeeException("Já foi aprovado o coffee break");
        }
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
        if(!isApproveCoffee.getIsApproveCoffee()) {
            throw new InvalidCoffeeException("Já foi reprovado o coffee break");
        }
    }

    public void validatorCancel(Long id,String email, List<String> groups){
        AuditoriumEntity entity = repository.findById(id).orElseThrow(() -> new ReservationIdIsNotFoundedException("Não foi encontrado a reserva"));
        Long idUser = userRepository.getIdByEmail(email);
        validator.isPossibleCanceled(entity.getStatus(),idUser,entity.getUser().getId(),groups);
    }

    // So vale pra quem pediu coffee. Os horarios ainda sao opcionais: cada regra so roda
    // com os horarios que vieram no body.
    private void validateCoffeeBreakTime(AuditoriumRequestDTO dto){
        if (!Boolean.TRUE.equals(dto.necessaryCoffee())) return;
        LocalTime setup = dto.foodSetupTime();
        LocalTime service = dto.serviceTime();

        if (setup != null && service != null && setup.isAfter(service)) {
            throw new InvalidTimeException("O Horário de montagem tem que ser antes do horário para servir");
        }
        if (setup != null && !(setup.isAfter(dto.timeOpenAuditorium()) && setup.isBefore(dto.endTime()))) {
            throw new InvalidTimeException("O horário para a montagem precisa estar dentro do horário da reserva");
        }
        if (service != null && !(service.isAfter(dto.timeOpenAuditorium()) && service.isBefore(dto.endTime()))){
            throw new InvalidTimeException("O horário para a servir precisa estar dentro do horário da reserva");
        }
        if (setup != null && ChronoUnit.MINUTES.between(setup, dto.endTime()) < 30) {
            throw new InvalidTimeException("É necessário que a montagem seja no mínimo 30 minutos antes do fim da reserva");
        }
        if (setup != null && service != null && ChronoUnit.MINUTES.between(setup, service) < 10){
            throw new InvalidTimeException("É necessário pelo menos 10 minutos de para a montagem da mesa");
        }
    }

}
