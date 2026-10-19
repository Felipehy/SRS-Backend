package com.siurbinfo.srs.dto.reservation.fleetVehicle;

import com.siurbinfo.srs.enums.VehicleType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public record FleetVehicleRequestDTO (

    @NotNull(message = "A data da reserva é obrigatória")
    @FutureOrPresent(message = "A data precisa ser atual ou futura")
     LocalDate reservationDate,

    @NotNull(message = "O horário de saída não pode ser null")
     LocalTime exitTime,

    @NotBlank(message = "O motivo não pode ser vazio")
     String reason,

    @NotBlank(message = "O endereço de saída não pode ser vazio")
    String departureAddress,

    @NotBlank(message = "O endereço de destino não pode ser vazio")
    String destinationAddress,

    @NotNull(message = "É necessário marcar SIM ou NÃO")
    Boolean isOutsideTheMunicipality,

    @Size(message = "Máximo de caracteres atingido(300)", max = 300)
    String reasonOutsideTheMunicipality,

    // se true, os 3 campos de retorno abaixo valem
    @NotNull(message = "É necessário um valor boleano")
     Boolean necessaryReturn,

     LocalTime returnTime,

     String departureAddressReturn,

     String destinationAddressReturn,

    @NotBlank(message = "O contato não pode ser vazio")
    @Size(max = 50, message = "O contato não pode ter mais de 50 caracteres")
     String contact,

    @NotBlank(message = "O nome do solicitante não pode ser null")
    String userNameRequester,

    @NotBlank(message = "O departamento do solicitante não pode ser null")
    @Size(max = 50, message = "O departamento não pode ter mais de 50 caracteres")
    String requesterDepartment,

    @NotNull(message = "É necessário um valor boleano")
     Boolean necessaryVan,

     String reasonUseVan,

    @NotNull(message = "O tipo de veículo não pode ser null")
     VehicleType typeVehicle,

    @NotNull(message = "O numero não pode ser null")
    @Positive(message = "A quantidade de passageiros precisa ser maior que 1")
     Integer quantityPassengers,

    // usuario para quem a reserva e feita (coluna id_user, V30).
    // Vem do body porque uma pessoa pode reservar em nome de outra.
    Long userId,

    @Size(max = 15, message = "O telefone não pode ter mais de 15 caracteres")
    String telephone
){}
