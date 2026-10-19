package com.siurbinfo.srs.dto.email;

import lombok.Builder;

import java.time.LocalDateTime;
import java.time.LocalTime;

// type e status vao como String de proposito: o template compara contra literal
// ('MEETING_ROOM', 'APROVADA'), e enum comparado com String no SpEL nunca casa.
//
// Campo opcional vai como null: o th:if de detailRow/noteRow some com a linha inteira,
// entao dado ausente nao vira linha em branco nem "null" no corpo do e-mail.
@Builder
public record ReservationEmailDTO(

        // ---- comuns aos tres tipos ----
        String protocol,
        String type,
        String status,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String purpose,            // so AUDITORIUM e FLEET_VEHICLE justificam o pedido
        String requesterName,
        String requesterSector,
        String requesterPhone,     // ramal do solicitante (coluna telephone)
        String observation,

        // ---- horario fora do expediente: MEETING_ROOM e AUDITORIUM ----
        String outsideHoursReason,

        // ---- cafe: MEETING_ROOM e AUDITORIUM (frota nao oferece) ----
        // Responde so "foi pedido?" no e-mail de solicitacao recebida; nos demais carrega
        // junto o estado da aprovacao. Ja vem formatado do factory ("Sim", "Nao", ...).
        String coffee,
        String coffeeReason,
        String coffeeRejectReason,

        // ---- MEETING_ROOM ----
        String floor,              // identifica a sala: e uma sala de reuniao por andar
        Integer attendees,

        // ---- AUDITORIUM ----
        // Sao tres horarios distintos: a abertura do auditorio, o inicio e o fim do evento.
        // Os dois ultimos ficam em startDateTime/endDateTime.
        LocalTime openingTime,
        String openingReason,
        Integer expectedAudience,  // total de pessoas
        // So quando ha publico externo. internalAudience e o total menos os externos.
        Integer internalAudience,
        Integer externalAudience,
        Boolean usesScreen,
        Boolean usesSoundSystem,

        // ---- FLEET_VEHICLE ----
        String vehicleModel,
        String licensePlate,
        String driverName,
        LocalTime departureTime,
        String departure,          // onde o motorista busca o passageiro
        String destination,
        Integer passengers,
        String contactPhone,       // telefone de contato, diferente do ramal acima
        String vanReason,          // so quando a reserva pede van
        // Diferente de hasReturn: aqui o "Nao" e informacao (a viagem fica no municipio),
        // e nao a ausencia de uma segunda viagem, entao a linha sai nos dois casos.
        Boolean outsideMunicipality,
        String outsideMunicipalityReason,  // so quando a viagem sai do municipio
        // Vem nulo quando a reserva nao pediu retorno: o e-mail entao nao fala de volta
        // nenhuma, em vez de gastar uma linha com "Retorno previsto: Nao".
        Boolean hasReturn,
        LocalTime returnTime,
        String returnDeparture,
        String returnDestination

) {
}
