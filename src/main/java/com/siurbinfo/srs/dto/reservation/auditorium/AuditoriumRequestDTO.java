package com.siurbinfo.srs.dto.reservation.auditorium;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalTime;

// Dados de entrada para solicitar uma reserva de auditorio
public record AuditoriumRequestDTO(

        // Data precisa ser hoje ou no futuro
        @NotNull(message = "A data da reserva é obrigatória")
        @FutureOrPresent(message = "A data precisa ser atual ou futura")
        LocalDate reservationDate,

        @NotNull(message = "O horário de abertura do auditório não pode ser null")
        LocalTime timeOpenAuditorium,

        @NotBlank(message = "O motivo não pode ser vazio")
        String reason,

        @NotNull(message = "O tempo inicial não pode ser null")
        LocalTime startTime,

        @NotNull(message = "O tempo final não pode ser null")
        LocalTime endTime,

        @NotBlank(message = "O nome do solicitante não pode ser null")
        String userNameRequester,

        // Precisa ser maior que zero
        @Positive(message = "O numero de pessoas precisa ser maior que 1")
        @NotNull(message = "O numero não pode ser null")
        Integer numPeople,

        @NotNull(message = "É necessário um valor boleano")
        Boolean externalPublic,

        @NotNull(message = "É necessário um valor boleano")
        Boolean necessaryScreen,

        @NotNull(message = "É necessário um valor boleano")
        Boolean necessarySoundSystem,

        @NotNull(message = "É necessário um valor boleano")
        Boolean necessaryCoffee

) {}
