package com.siurbinfo.srs.dto.reservation.auditorium;

import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Resposta com os dados de uma reserva de auditorio
public record AuditoriumResponseDTO(
        Long id,
        LocalDate reservationDate,
        LocalDateTime dateAnalysis,
        LocalTime timeOpenAuditorium,
        ReserveType reservationType,
        String reason,
        String reasonFailure,
        LocalTime startTime,
        LocalTime endTime,
        String userNameRequester,
        String userNameApprover,
        Integer numPeople,
        Boolean externalPublic,
        Boolean necessaryScreen,
        Boolean necessarySoundSystem,
        Boolean necessaryCoffee,
        String observation,
        ReserveStatus status,
        LocalDateTime dateCreate
) {}
