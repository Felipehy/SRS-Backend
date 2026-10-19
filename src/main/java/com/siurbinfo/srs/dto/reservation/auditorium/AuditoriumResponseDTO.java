package com.siurbinfo.srs.dto.reservation.auditorium;

import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record AuditoriumResponseDTO(
        Long id,
        LocalDate reservationDate,
        Instant dateAnalysis,
        LocalTime timeOpenAuditorium,
        ReserveType reservationType,
        String reason,
        String reasonFailure,
        LocalTime startTime,
        String reasonOutsideHours,
        LocalTime endTime,
        String userNameRequester,
        String requesterDepartment,
        String userNameApprover,
        Integer numPeople,
        Boolean externalPublic,
        Boolean necessaryScreen,
        Boolean necessarySoundSystem,
        CoffeeBreakDTO coffeeBreak,
        String observation,
        ReserveStatus status,
        Instant dateCreate,
        Long idTicket,
        Long userId,
        String telephone,
        Integer numExternalPeople,
        String reasonOpenAuditorium,
        Instant dateCancellation,
        String reasonsCancellation
) {}
