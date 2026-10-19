package com.siurbinfo.srs.dto.reservation.fleetVehicle;

import com.siurbinfo.srs.dto.driver.DriverResponseDTO;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.enums.VehicleType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record FleetVehicleResponseDTO (
        
     Long id,
     LocalDate reservationDate,
     Instant dateAnalysis,
     ReserveType reservationType,
     LocalTime exitTime,
     String reason,
     String reasonFailure,
     String departureAddress,
     String destinationAddress,
     Boolean isOutsideTheMunicipality,
     String reasonOutsideTheMunicipality,
     DriverResponseDTO driver,
     String userNameRequester,
     String requesterDepartment,
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
     Instant dateCreate,
     Long userId,
     String telephone,
     Instant dateCancellation,
     String reasonsCancellation

){}
