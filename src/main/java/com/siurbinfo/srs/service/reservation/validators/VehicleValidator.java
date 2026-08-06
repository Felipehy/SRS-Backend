package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleRequestDTO;
import com.siurbinfo.srs.entity.Reserve.FleetVehicleEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.exception.SameDepartureAndDestinationException;
import com.siurbinfo.srs.repository.reservation.ReservationFleetVehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

// Validador de regras de negocio para reservas de veiculo da frota: conflito de horario
// de saida e verificacao de que origem e destino nao sao o mesmo endereco.
@Component
public class VehicleValidator {

    @Autowired
    ReservationValidator<FleetVehicleEntity> validator;

    @Autowired
    ReservationFleetVehicleRepository repository;

    // Valida a criacao de uma reserva de veiculo.
    public void validateCreate(FleetVehicleRequestDTO dto){
        // conflito: ja existe reserva de veiculo nesse mesmo dia/horario de saida
        if (repository.existsByReservationDateAndHour(dto.reservationDate(),dto.exitTime())){
            throw new ReservationConflictException("Já existe uma reserva nesta sala para esse dia");
        }
        // regra: endereco de partida nao pode ser igual ao endereco de destino
        if (dto.departureAddress().equals(dto.destinationAddress())){
            throw new SameDepartureAndDestinationException("O endereço de partida é igual ao endereço de destino");
        }
    }

    // Valida a atualizacao de uma reserva de veiculo existente.
    public void validateUpdate(FleetVehicleRequestDTO dto,FleetVehicleEntity entity){
        validator.isSameReservationDate(entity.getReservationDate(),dto.reservationDate());
        // regra: endereco de partida nao pode ser igual ao endereco de destino
        if (dto.departureAddress().equals(dto.destinationAddress())){throw new SameDepartureAndDestinationException("O endereço de partida é igual ao endereço de destino");}
        // conflito: ja existe reserva de veiculo nesse mesmo dia/horario de saida
        if (repository.existsByReservationDateAndHour(dto.reservationDate(),dto.exitTime())){throw new ReservationConflictException("Já existe uma reserva nesta sala para esse dia");}
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
