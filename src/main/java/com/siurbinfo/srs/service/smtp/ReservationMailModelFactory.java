package com.siurbinfo.srs.service.smtp;

import com.siurbinfo.srs.dto.email.ReservationEmailDTO;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * ATENCAO - transacao: room (MeetingRoomEntity) e driver (FleetVehicleEntity) sao LAZY.
 * Chame estes metodos DENTRO da transacao que carregou a entidade; fora dela da
 * LazyInitializationException.
 *
 * <p>Campos marcados com FALTA nao tem origem nas entidades de hoje. Vao como null de
 * proposito: o {@code th:if} do fragmento detailRow esconde a linha inteira (rotulo junto),
 * entao o e-mail sai mais enxuto em vez de sair com "null" ou linha em branco.
 */
@Component
public class ReservationMailModelFactory {

    public static final String TEMPLATE_REQUEST = "email/request_email";
    public static final String TEMPLATE_APPROVE = "email/approve_email";
    public static final String TEMPLATE_REJECT  = "email/reject_email";
    public static final String TEMPLATE_CANCEL  = "email/cancel_email";
    public static final String TEMPLATE_FINISH  = "email/finish_email";
    public static final String TEMPLATE_COFFEE_BREAK = "email/coffee_break_email";

    // dateCreate e dateAnalysis sao Instant, e #temporals.format(instant, 'dd/MM/yyyy HH:mm')
    // estoura no template: Instant nao tem campos de data. A conversao acontece aqui.
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    // toRequestedDto, e nao toDto: neste e-mail o cafe aparece so como pedido, sem aprovacao.
    public Map<String, Object> requested(String username, String url, String supportEmail, MeetingRoomEntity entity) {
        return requested(username, url, supportEmail, toRequestedDto(entity), entity.getDateCreate());
    }

    public Map<String, Object> requested(String username, String url, String supportEmail, AuditoriumEntity entity) {
        return requested(username, url, supportEmail, toRequestedDto(entity), entity.getDateCreate());
    }

    public Map<String, Object> requested(String username, String url, String supportEmail, FleetVehicleEntity entity) {
        return requested(username, url, supportEmail, toDto(entity), entity.getDateCreate());
    }

    public Map<String, Object> approved(String username, String url, String supportEmail, String nameRequester, MeetingRoomEntity entity) {
        return approved(username, url, supportEmail, toDto(entity), nameRequester, entity.getDateAnalysis());
    }

    public Map<String, Object> approved(String username, String url, String supportEmail, String nameRequester, AuditoriumEntity entity) {
        return approved(username, url, supportEmail, toDto(entity), nameRequester, entity.getDateAnalysis());
    }

    public Map<String, Object> approved(String username, String url, String supportEmail, String nameRequester, FleetVehicleEntity entity) {
        return approved(username, url, supportEmail, toDto(entity), nameRequester, entity.getDateAnalysis());
    }

    public Map<String, Object> rejected(String username, String url, String supportEmail, String nameRequester, MeetingRoomEntity entity) {
        return rejected(username, url, supportEmail, toDto(entity), nameRequester,
                entity.getDateAnalysis(), entity.getReasonFailure());
    }

    public Map<String, Object> rejected(String username, String url, String supportEmail, String nameRequester, AuditoriumEntity entity) {
        return rejected(username, url, supportEmail, toDto(entity), nameRequester,
                entity.getDateAnalysis(), entity.getReasonFailure());
    }

    public Map<String, Object> rejected(String username, String url, String supportEmail, String nameRequester, FleetVehicleEntity entity) {
        return rejected(username, url, supportEmail, toDto(entity), nameRequester,
                entity.getDateAnalysis(), entity.getReasonFailure());
    }

    public Map<String, Object> cancelled(String username, String url, String supportEmail, MeetingRoomEntity entity) {
        return cancelled(username, url, supportEmail, toDto(entity));
    }

    public Map<String, Object> cancelled(String username, String url, String supportEmail, AuditoriumEntity entity) {
        return cancelled(username, url, supportEmail, toDto(entity));
    }

    public Map<String, Object> cancelled(String username, String url, String supportEmail, FleetVehicleEntity entity) {
        return cancelled(username, url, supportEmail, toDto(entity));
    }

    public Map<String, Object> finished(String username, String url, String supportEmail, MeetingRoomEntity entity) {
        return finished(username, url, supportEmail, toDto(entity));
    }

    public Map<String, Object> finished(String username, String url, String supportEmail, AuditoriumEntity entity) {
        return finished(username, url, supportEmail, toDto(entity));
    }

    public Map<String, Object> finished(String username, String url, String supportEmail, FleetVehicleEntity entity) {
        return finished(username, url, supportEmail, toDto(entity));
    }

    public Map<String, Object> coffeeBreakApproved(AuditoriumEntity entity, String supportEmail){
        return coffeeBreakApproved(supportEmail, entity);
    }

    // HashMap, e nao Map.of: os campos sem origem entram como null e Map.of estoura com null.
    private Map<String, Object> base(String username, String url, String supportEmail, ReservationEmailDTO reservation) {
        Map<String, Object> model = new HashMap<>();
        model.put("username", username);
        model.put("urlAccompany", url);
        model.put("reservation", reservation);
        model.put("email", supportEmail);
        return model;
    }

    public Map<String, Object> requested(String username, String url, String supportEmail,
                                         ReservationEmailDTO reservation,
                                         Instant requestedAt) {
        Map<String, Object> model = base(username, url, supportEmail, reservation);
        model.put("requestedAt", toLocalDateTime(requestedAt));
        return model;
    }

    public Map<String, Object> approved(String username, String url, String supportEmail,
                                        ReservationEmailDTO reservation,
                                        String approvedBy, Instant approvedAt) {
        Map<String, Object> model = base(username, url, supportEmail, reservation);
        model.put("approvedBy", approvedBy);
        model.put("approvedAt", toLocalDateTime(approvedAt));
        return model;
    }

    public Map<String, Object> rejected(String username, String url, String supportEmail, ReservationEmailDTO reservation,
                                        String rejectedBy, Instant rejectedAt, String rejectReason) {
        Map<String, Object> model = base(username, url, supportEmail, reservation);
        model.put("rejectedBy", rejectedBy);
        model.put("rejectedAt", toLocalDateTime(rejectedAt));
        model.put("rejectReason", rejectReason);
        return model;
    }

    public Map<String, Object> cancelled(String username, String url, String supportEmail, ReservationEmailDTO reservation) {
        Map<String, Object> model = base(username, url, supportEmail, reservation);
        // FALTA: quem cancelou. dateAnalysis/userNameApprover sao da analise (aprovar ou
        // reprovar), nao do cancelamento, entao reaproveitar aqui rotularia errado.
        model.put("cancelledBy", null);
        model.put("cancelledAt", null);
        // FALTA: motivo do cancelamento. reasonFailure existe, mas e o motivo de REPROVADA,
        // que e outro estado do ReserveStatus.
        model.put("cancelReason", null);
        return model;
    }

    public Map<String, Object> finished(String username, String url, String supportEmail, ReservationEmailDTO reservation) {
        Map<String, Object> model = base(username, url, supportEmail, reservation);
        // FALTA: as entidades nao registram finalizacao, so o status FINALIZADA.
        model.put("finishedAt", null);
        model.put("finishNotes", null);
        return model;
    }

    public Map<String, Object> coffeeBreakApproved(String supportEmail, AuditoriumEntity entity){
        Map<String, Object> model = new HashMap<>();
        model.put("supportEmail", supportEmail);
        model.put("numPeople", entity.getNumPeople());
        model.put("subjectEvent", entity.getReason());
        model.put("date", entity.getReservationDate());
        model.put("foodSetupTime", entity.getFoodSetupTime());
        model.put("serviceTime", entity.getServiceTime());
        model.put("locale", "Auditório da Secretaria Municipal de Infraestrutura Urbana e Obras – SIURB");
        model.put("address", "Rua XV de Novembro, 165 – Centro Histórico – São Paulo/SP");
        return model;
    }

    public ReservationEmailDTO toDto(MeetingRoomEntity entity) {
        return meetingRoom(entity)
                .coffee(coffeeDecision(Boolean.TRUE.equals(entity.getIsCoffee()),
                        entity.getIsApproveCoffee()))
                .coffeeRejectReason(coffeeRejectReason(Boolean.TRUE.equals(entity.getIsCoffee()),
                        entity.getIsApproveCoffee(), entity.getReasonRejectCoffee()))
                .build();
    }

    public ReservationEmailDTO toRequestedDto(MeetingRoomEntity entity) {
        return meetingRoom(entity)
                .coffee(coffeeRequest(Boolean.TRUE.equals(entity.getIsCoffee())))
                .build();
    }

    private ReservationEmailDTO.ReservationEmailDTOBuilder meetingRoom(MeetingRoomEntity entity) {
        return ReservationEmailDTO.builder()
                .protocol(protocol(entity.getId()))
                .type(entity.getReservationType() == null ? null : entity.getReservationType().name())
                .status(entity.getStatus() == null ? null : entity.getStatus().name())
                .startDateTime(at(entity.getReservationDate(), entity.getStartTime()))
                .endDateTime(at(entity.getReservationDate(), entity.getEndTime()))
                // De proposito: reserva de sala de reuniao nao tem finalidade. So auditorio e
                // frota precisam justificar o pedido (campo reason nessas entidades), por isso
                // MeetingRoomEntity nao tem reason. A linha "Finalidade" some no e-mail de sala.
                .purpose(null)
                .requesterName(entity.getUserNameRequester())
                .requesterSector(entity.getRequesterDepartment())
                .requesterPhone(entity.getTelephone())
                .observation(entity.getObservation())
                .outsideHoursReason(entity.getReasonOutsideHours())
                .coffeeReason(coffeeReason(Boolean.TRUE.equals(entity.getIsCoffee()), entity.getReasonCoffee()))
                .floor(entity.getRoom() == null ? null : entity.getRoom().getFloor())
                .attendees(entity.getNumPeople());
    }

    public ReservationEmailDTO toDto(AuditoriumEntity entity) {
        return auditorium(entity)
                .coffee(coffeeDecision(entity.isNecessaryCoffee(), entity.getIsApproveCoffeeBreak()))
                .coffeeRejectReason(coffeeRejectReason(entity.isNecessaryCoffee(),
                        entity.getIsApproveCoffeeBreak(), entity.getReasonRejectCoffee()))
                .build();
    }

    public ReservationEmailDTO toRequestedDto(AuditoriumEntity entity) {
        return auditorium(entity)
                .coffee(coffeeRequest(entity.isNecessaryCoffee()))
                .build();
    }

    private ReservationEmailDTO.ReservationEmailDTOBuilder auditorium(AuditoriumEntity entity) {
        return ReservationEmailDTO.builder()
                .protocol(protocol(entity.getId()))
                .type(entity.getReservationType() == null ? null : entity.getReservationType().name())
                .status(entity.getStatus() == null ? null : entity.getStatus().name())
                .startDateTime(at(entity.getReservationDate(), entity.getStartTime()))
                .endDateTime(at(entity.getReservationDate(), entity.getEndTime()))
                .purpose(entity.getReason())
                .requesterName(entity.getUserNameRequester())
                .requesterSector(entity.getRequesterDepartment())
                .requesterPhone(entity.getTelephone())
                .observation(entity.getObservation())
                .outsideHoursReason(entity.getReasonOutsideHours())
                .coffeeReason(coffeeReason(entity.isNecessaryCoffee(), entity.getReasonCoffee()))
                .openingTime(entity.getTimeOpenAuditorium())
                .openingReason(entity.getReasonOpenAuditorium())
                .expectedAudience(entity.getNumPeople())
                .internalAudience(internalAudience(entity))
                .externalAudience(entity.isExternalPublic() ? entity.getNumExternalPeople() : null)
                .usesScreen(entity.isNecessaryScreen())
                .usesSoundSystem(entity.isNecessarySoundSystem());
    }

    public ReservationEmailDTO toDto(FleetVehicleEntity entity) {
        return ReservationEmailDTO.builder()
                .protocol(protocol(entity.getId()))
                .type(entity.getReservationType() == null ? null : entity.getReservationType().name())
                .status(entity.getStatus() == null ? null : entity.getStatus().name())
                .startDateTime(at(entity.getReservationDate(), entity.getExitTime()))
                .endDateTime(entity.isNecessaryReturn()
                        ? at(entity.getReservationDate(), entity.getReturnTime())
                        : null)
                .purpose(entity.getReason())
                .requesterName(entity.getUserNameRequester())
                .requesterSector(entity.getRequesterDepartment())
                .requesterPhone(entity.getTelephone())
                .contactPhone(entity.getContact())
                // Modelo e placa vem do motorista (tbl_driver). Sem motorista vinculado sobra
                // so a categoria do veiculo (VAN / CARRO), que ja e melhor que linha nenhuma.
                .vehicleModel(vehicleModel(entity))
                .licensePlate(entity.getDriver() == null ? null : entity.getDriver().getLicensePlate())
                .driverName(entity.getDriver() == null ? null : entity.getDriver().getName())
                .departureTime(entity.getExitTime())
                .departure(entity.getDepartureAddress())
                .destination(entity.getDestinationAddress())
                .passengers(entity.getQuantityPassengers())
                .vanReason(entity.isNecessaryVan() ? entity.getReasonUseVan() : null)
                .outsideMunicipality(entity.getIsOutsideTheMunicipality())
                // Mesma regra da van: sem a viagem sair do municipio o motivo nao vai, que
                // a coluna pode ter sobra de um rascunho em que a caixa chegou a ser marcada.
                .outsideMunicipalityReason(Boolean.TRUE.equals(entity.getIsOutsideTheMunicipality())
                        ? entity.getReasonOutsideTheMunicipality()
                        : null)
                .hasReturn(entity.isNecessaryReturn() ? Boolean.TRUE : null)
                // Mesma regra do endDateTime: as colunas podem ter sobra de um rascunho
                // anterior e nao devem ir para o e-mail.
                .returnTime(entity.isNecessaryReturn() ? entity.getReturnTime() : null)
                .returnDeparture(entity.isNecessaryReturn() ? entity.getDepartureAddressReturn() : null)
                .returnDestination(entity.isNecessaryReturn() ? entity.getDestinationAddressReturn() : null)
                .build();
    }

    // Nao existe campo de protocolo: o numero exibido e o id com zero a esquerda.
    private String protocol(Long id) {
        return id == null ? null : String.format("%06d", id);
    }

    private String coffeeRequest(boolean requested) {
        return requested ? "Sim" : "Não";
    }

    // Cafe dos demais e-mails: alem de ter sido pedido, carrega o estado da aprovacao,
    // senao um e-mail de reserva aprovada prometeria cafe que foi reprovado. O porque da
    // recusa fica em coffeeRejectReason, em linha propria.
    private String coffeeDecision(boolean requested, Boolean approved) {
        if (!requested) {
            return "Não";
        }
        // approved nulo = pedido ainda nao analisado.
        if (approved == null) {
            return "Sim (aguardando aprovação)";
        }
        return approved ? "Sim (aprovado)" : "Sim (reprovado)";
    }

    private String coffeeRejectReason(boolean requested, Boolean approved, String rejectReason) {
        if (!requested || !Boolean.FALSE.equals(approved) || !isFilled(rejectReason)) {
            return null;
        }
        return rejectReason;
    }

    // Sem cafe pedido nao ha motivo a mostrar, mesmo que a coluna tenha sobra de um
    // rascunho em que o cafe chegou a ser marcado.
    private String coffeeReason(boolean requested, String reason) {
        return (requested && isFilled(reason)) ? reason : null;
    }

    // Pessoas internas sao o resto do total. Total menor que os externos so acontece com
    // dado inconsistente: nesse caso a linha some, em vez de anunciar numero negativo.
    private Integer internalAudience(AuditoriumEntity entity) {
        if (!entity.isExternalPublic()
                || entity.getNumPeople() == null
                || entity.getNumExternalPeople() == null) {
            return null;
        }
        int internal = entity.getNumPeople() - entity.getNumExternalPeople();
        return internal < 0 ? null : internal;
    }

    private boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

    private String vehicleModel(FleetVehicleEntity entity) {
        if (entity.getDriver() != null && entity.getDriver().getModelCar() != null) {
            return entity.getDriver().getModelCar();
        }
        return entity.getTypeVehicle() == null ? null : entity.getTypeVehicle().name();
    }

    private LocalDateTime at(LocalDate date, LocalTime time) {
        return (date == null || time == null) ? null : date.atTime(time);
    }

    public LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZONE);
    }

}
