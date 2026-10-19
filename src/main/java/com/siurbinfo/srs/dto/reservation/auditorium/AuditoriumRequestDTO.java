package com.siurbinfo.srs.dto.reservation.auditorium;

import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record AuditoriumRequestDTO(

        @NotNull(message = "A data da reserva é obrigatória")
        @FutureOrPresent(message = "A data precisa ser atual ou futura")
        LocalDate reservationDate,

        @NotNull(message = "O horário de abertura do auditório não pode ser null")
        LocalTime timeOpenAuditorium,

        @NotBlank(message = "O motivo não pode ser vazio")
        String reason,

        @Size(max = 300, message = "Máximo de caracteres atingido(300)")
        String reasonOutsideHours,

        @NotNull(message = "O tempo inicial não pode ser null")
        LocalTime startTime,

        @NotNull(message = "O tempo final não pode ser null")
        LocalTime endTime,

        @NotBlank(message = "O nome do solicitante não pode ser null")
        String userNameRequester,

        @NotBlank(message = "O departamento do solicitante não pode ser null")
        @Size(max = 50, message = "O departamento não pode ter mais de 50 caracteres")
        String requesterDepartment,

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
        Boolean necessaryCoffee,

        LocalTime foodSetupTime,

        LocalTime serviceTime,

        @Size(message = "A quantidade de caracteres ultrapassou o limite (250)", max = 250)
        String reasonCoffee,

        Long userId,

        @Size(max = 15, message = "O telefone não pode ter mais de 15 caracteres")
        String telephone,

        @PositiveOrZero(message = "O numero de pessoas externas não pode ser negativo")
        Integer numExternalPeople,

        @Size(max = 1000, message = "Limite de caracteres atingido!")
        String observation,

        // Por que o auditorio abre nesse horario. Opcional: quando a abertura coincide com
        // o inicio do evento nao ha o que justificar, e a linha nem sai no e-mail.
        @Size(max = 255, message = "Limite de caracteres atingido!")
        String reasonOpenAuditorium
) {}
