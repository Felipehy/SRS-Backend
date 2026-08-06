package com.siurbinfo.srs.dto.reservation.meetingRoom;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

// Dados de entrada para solicitar uma reserva de sala de reuniao
public record MeetingRoomRequestDTO(

        // Data precisa ser hoje ou no futuro
        @NotNull(message = "A data da reserva é obrigatória")
        @FutureOrPresent(message = "A data precisa ser atual ou futura")
        LocalDate reservationDate,

        @NotNull(message = "O id da sala não pode ser null")
        Long roomId,

        @NotNull(message = "O tempo inicial não pode ser null")
        LocalTime startTime,

        @NotBlank(message = "O nome do solicitante não pode ser null")
        String userNameRequester,

        @NotNull(message = "O tempo final não pode ser null")
        LocalTime endTime,

        // Precisa ser maior que zero
        @NotNull(message = "O numero não pode ser null")
        @Positive(message = "O numero de pessoas precisa ser maior que 1")
        Integer numPeople,

        @NotNull(message = "É necessário um valor boleano")
        Boolean isCoffee,

        String observation

) {
}