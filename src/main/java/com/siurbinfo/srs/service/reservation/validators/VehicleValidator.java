package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleRequestDTO;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.exception.InvalidReasonException;
import com.siurbinfo.srs.exception.InvalidReservationException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.SameDepartureAndDestinationException;
import com.siurbinfo.srs.repository.reservation.ReservationFleetVehicleRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VehicleValidator {

    private final ReservationValidator<FleetVehicleEntity> validator;
    private final ReservationFleetVehicleRepository repository;
    private final UserRepository userRepository;

    public void validateCreate(FleetVehicleRequestDTO dto, List<String> groups){
        validateRequest(dto,groups);
    }

    // No update a data precisa mudar; o resto do body passa pelas mesmas regras do create.
    public void validateUpdate(FleetVehicleRequestDTO dto,FleetVehicleEntity entity, List<String> groups){
        validator.isSameReservationDate(entity.getReservationDate(),dto.reservationDate());
        validateRequest(dto,groups);
    }

    private void validateRequest(FleetVehicleRequestDTO dto, List<String> groups){

        Boolean isBlankReasonOutsideTheMunicipality = dto.reasonOutsideTheMunicipality() == null || dto.reasonOutsideTheMunicipality().isBlank();
        Boolean isBlankReasonUseVan = dto.reasonUseVan() == null || dto.reasonUseVan().isBlank();

        if (dto.departureAddress().equals(dto.destinationAddress())){
            throw new SameDepartureAndDestinationException("O endereço de partida é igual ao endereço de destino");
        }
        if (dto.isOutsideTheMunicipality() && isBlankReasonOutsideTheMunicipality){
            throw new InvalidReasonException("É necessário colocar o motivo do endereço fora do município");
        }

        if (dto.quantityPassengers() <= 4 && dto.necessaryVan()){
            throw new InvalidReservationException("Quantidade mínima para van não atingida (5 pessoas)");
        }

        if (dto.necessaryVan() && isBlankReasonUseVan){
            throw new InvalidReasonException("É necessário colocar um motivo para pedir a van");
        }

        if (validator.isSpecialUser(groups)) return;
        validator.isStartiTimeStartAfterATime(dto.reservationDate(),dto.exitTime(),120,repository);
        validator.validateIntervalDays(dto.reservationDate(),2);
    }

    public void validatorApprove(Long id){
        ReserveStatus status = repository.findStatusById(id);
        validator.isPossibleApproveOrReject(status);
    }

    public void validatorReject(Long id){
        ReserveStatus status = repository.findStatusById(id);
        validator.isPossibleApproveOrReject(status);
    }

    public void validatorCancel(Long id,String email, List<String> groups){
        FleetVehicleEntity entity = repository.findById(id).orElseThrow(() -> new ReservationIdIsNotFoundedException("Não foi encontrado a reserva"));
        Long idUser = userRepository.getIdByEmail(email);
        validator.isPossibleCanceled(entity.getStatus(),idUser,entity.getUser().getId(),groups);
    }

}
