package com.siurbinfo.srs.dto.reservation.fleetVehicle;

import com.siurbinfo.srs.enums.VehicleType;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

// Dados de entrada para solicitar uma reserva de veiculo da frota
public record FleetVehicleRequestDTO (

    // Data precisa ser hoje ou no futuro
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

    // Indica se a viagem tem retorno; se true, os campos de retorno abaixo sao usados
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

    // Indica se e necessaria uma van; se true, reasonUseVan explica o motivo
    @NotNull(message = "É necessário um valor boleano")
     Boolean necessaryVan,

     String reasonUseVan,

    @NotNull(message = "O tipo de veículo não pode ser null")
     VehicleType typeVehicle,

    // Precisa ser maior que zero
    @NotNull(message = "O numero não pode ser null")
    @Positive(message = "A quantidade de passageiros precisa ser maior que 1")
     Integer quantityPassengers
){}
