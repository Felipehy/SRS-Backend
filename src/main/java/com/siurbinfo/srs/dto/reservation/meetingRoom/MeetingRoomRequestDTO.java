package com.siurbinfo.srs.dto.reservation.meetingRoom;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;

public record MeetingRoomRequestDTO(

        @NotNull(message = "A data da reserva é obrigatória")
        @FutureOrPresent(message = "A data precisa ser atual ou futura")
        LocalDate reservationDate,

        @NotNull(message = "O id da sala não pode ser null")
        Long roomId,

        @NotNull(message = "O tempo inicial não pode ser null")
        LocalTime startTime,

        @Size(max = 300, message = "Máximo de caracteres atingido(300)")
        String reasonOutsideHours,

        @NotBlank(message = "O nome do solicitante não pode ser null")
        String userNameRequester,

        @NotBlank(message = "O departamento do solicitante não pode ser null")
        @Size(max = 50, message = "O departamento não pode ter mais de 50 caracteres")
        String requesterDepartment,

        @NotNull(message = "O tempo final não pode ser null")
        LocalTime endTime,

        @NotNull(message = "O numero não pode ser null")
        @Positive(message = "O numero de pessoas precisa ser maior que 1")
        Integer numPeople,

        Boolean isCoffee,

        @Size(message = "A quantidade de caracteres ultrapassou o limite (250)", max = 250)
        String reasonCoffee,

        String observation,

        // usuario para quem a reserva e feita (coluna id_user, V30).
        // Vem do body porque uma pessoa pode reservar em nome de outra.
        Long userId,

        @Size(max = 15, message = "O telefone não pode ter mais de 15 caracteres")
        String telephone

) {
}