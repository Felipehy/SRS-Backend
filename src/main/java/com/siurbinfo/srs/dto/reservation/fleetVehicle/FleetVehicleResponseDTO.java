package com.siurbinfo.srs.dto.reservation.fleetVehicle;

import com.siurbinfo.srs.dto.driver.DriverResponseDTO;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.enums.VehicleType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Resposta com os dados de uma reserva de veiculo da frota
public record FleetVehicleResponseDTO (
        
     Long id,

     LocalDate reservationDate,

     LocalDateTime dateAnalysis,

     ReserveType reservationType,

     LocalTime exitTime,

     String reason,

     String reasonFailure,

     String departureAddress,

     String destinationAddress,

     DriverResponseDTO driver,

     String userNameRequester,

     String userNameApprover,

     Boolean necessaryReturn,

     LocalTime returnTime,

     String departureAddressReturn,

     String destinationAddressReturn,

     String contact,

     Boolean necessaryVan,

     String reasonUseVan,

     VehicleType typeVehicle,

     Integer quantityPassengers,

     ReserveStatus status,

     LocalDateTime dateCreate
){}
