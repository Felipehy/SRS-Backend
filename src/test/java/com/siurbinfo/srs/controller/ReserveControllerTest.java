package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.reservation.AuditoriumController;
import com.siurbinfo.srs.controller.reservation.FleetVehicleController;
import com.siurbinfo.srs.controller.reservation.MeetingRoomController;
import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumResponseDTO;
import com.siurbinfo.srs.dto.reservation.cancellation.CancellationDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleResponseDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomResponseDTO;
import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.ReservationWasCancell;
import com.siurbinfo.srs.exception.SameDepartureAndDestinationException;
import com.siurbinfo.srs.exception.StartTimeIsNotBeforeEndTimeException;
import com.siurbinfo.srs.service.log.LogSystemService;
import com.siurbinfo.srs.service.reservation.AuditoriumService;
import com.siurbinfo.srs.service.reservation.MeetingRoomService;
import com.siurbinfo.srs.service.reservation.VehicleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuditoriumController.class, FleetVehicleController.class, MeetingRoomController.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ReserveControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuditoriumService auditoriumService;

    @MockitoBean
    VehicleService vehicleService;

    @MockitoBean
    MeetingRoomService meetingRoomService;

    @MockitoBean
    LogSystemService logSystemService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private static final LocalDate DATE = LocalDate.now();

    private static final String MEETING_JSON = String.format("""
            {
              "reservationDate": "%s",
              "roomId": 1,
              "startTime": "09:00:00",
              "userNameRequester": "Fulano",
              "requesterDepartment": "TI",
              "endTime": "10:00:00",
              "numPeople": 5,
              "isCoffee": true,
              "observation": "obs",
              "userId": 7,
              "telephone": "1140028922"
            }""",DATE);

    private static final String AUDITORIUM_JSON = String.format("""
            {
              "reservationDate": "%s",
              "timeOpenAuditorium": "08:30:00",
              "reason": "Palestra",
              "startTime": "09:00:00",
              "endTime": "11:00:00",
              "userNameRequester": "Fulano",
              "requesterDepartment": "TI",
              "numPeople": 50,
              "externalPublic": false,
              "necessaryScreen": true,
              "necessarySoundSystem": true,
              "necessaryCoffee": false,
              "userId": 7,
              "telephone": "1140028922",
              "numExternalPeople": 10
            }""", DATE);

    private static final String VEHICLE_JSON = String.format("""
            {
              "reservationDate": "%s",
              "exitTime": "08:00:00",
              "reason": "Visita",
              "departureAddress": "Sede",
              "destinationAddress": "Cliente X",
              "isOutsideTheMunicipality": false,
              "necessaryReturn": true,
              "returnTime": "18:00:00",
              "departureAddressReturn": "Cliente X",
              "destinationAddressReturn": "Sede",
              "contact": "11999998888",
              "userNameRequester": "Fulano",
              "requesterDepartment": "TI",
              "necessaryVan": false,
              "reasonUseVan": null,
              "typeVehicle": "CARRO",
              "quantityPassengers": 3,
              "userId": 7,
              "telephone": "1140028922"
            }""", DATE);

    private static MeetingRoomResponseDTO meetingRoomResponseWithId() {
        return new MeetingRoomResponseDTO(1L, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static AuditoriumResponseDTO auditoriumResponseWithId() {
        return AuditoriumResponseDTO.builder().id(1L).build();
    }

    private static FleetVehicleResponseDTO fleetVehicleResponseWithId() {
        return new FleetVehicleResponseDTO(1L, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
    }

    @Test
    void createMeetingRoom_returns201() throws Exception {
        given(meetingRoomService.createReservation(any(), any())).willReturn(meetingRoomResponseWithId());

        mvc.perform(post("/api/reservation/meeting-room")
                        .contentType(APPLICATION_JSON)
                        .content(MEETING_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void createAuditorium_returns201() throws Exception {
        given(auditoriumService.createReservation(any(), any())).willReturn(auditoriumResponseWithId());

        mvc.perform(post("/api/reservation/auditorium")
                        .contentType(APPLICATION_JSON)
                        .content(AUDITORIUM_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void createFleetVehicle_returns201() throws Exception {
        given(vehicleService.createReservation(any(), any())).willReturn(fleetVehicleResponseWithId());

        mvc.perform(post("/api/reservation/fleet-vehicle")
                        .contentType(APPLICATION_JSON)
                        .content(VEHICLE_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void createMeetingRoom_invalidBody_returns400() throws Exception {
        mvc.perform(post("/api/reservation/meeting-room")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMeetingRoom_semRequesterDepartment_returns400() throws Exception {
        mvc.perform(post("/api/reservation/meeting-room")
                        .contentType(APPLICATION_JSON)
                        .content(MEETING_JSON.replace("\"requesterDepartment\": \"TI\",", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMeetingRoom_requesterDepartmentAcimaDe50Caracteres_returns400() throws Exception {
        String departamentoLongo = "A".repeat(51);

        mvc.perform(post("/api/reservation/meeting-room")
                        .contentType(APPLICATION_JSON)
                        .content(MEETING_JSON.replace(
                                "\"requesterDepartment\": \"TI\"",
                                "\"requesterDepartment\": \"" + departamentoLongo + "\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMeetingRoom_telefoneAcimaDe15Caracteres_returns400() throws Exception {
        String telefoneLongo = "9".repeat(16);

        mvc.perform(post("/api/reservation/meeting-room")
                        .contentType(APPLICATION_JSON)
                        .content(MEETING_JSON.replace(
                                "\"telephone\": \"1140028922\"",
                                "\"telephone\": \"" + telefoneLongo + "\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAuditorium_numExternalPeopleNegativo_returns400() throws Exception {
        mvc.perform(post("/api/reservation/auditorium")
                        .contentType(APPLICATION_JSON)
                        .content(AUDITORIUM_JSON.replace(
                                "\"numExternalPeople\": 10",
                                "\"numExternalPeople\": -1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAuditorium_novosCamposChegamNoService() throws Exception {
        given(auditoriumService.createReservation(any(), any())).willReturn(auditoriumResponseWithId());

        mvc.perform(post("/api/reservation/auditorium")
                        .contentType(APPLICATION_JSON)
                        .content(AUDITORIUM_JSON))
                .andExpect(status().isCreated());

        ArgumentCaptor<AuditoriumRequestDTO> captor = ArgumentCaptor.forClass(AuditoriumRequestDTO.class);
        verify(auditoriumService).createReservation(captor.capture(), any());

        AuditoriumRequestDTO enviado = captor.getValue();
        assertEquals(7L, enviado.userId());
        assertEquals("1140028922", enviado.telephone());
        assertEquals(10, enviado.numExternalPeople());
    }

    @Test
    void createMeetingRoom_conflict_returns409() throws Exception {
        doThrow(new ReservationConflictException("conflito"))
                .when(meetingRoomService).createReservation(any(), any());

        mvc.perform(post("/api/reservation/meeting-room")
                        .contentType(APPLICATION_JSON)
                        .content(MEETING_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void createAuditorium_invalidTime_returns400() throws Exception {
        doThrow(new StartTimeIsNotBeforeEndTimeException("tempo inválido"))
                .when(auditoriumService).createReservation(any(), any());

        mvc.perform(post("/api/reservation/auditorium")
                        .contentType(APPLICATION_JSON)
                        .content(AUDITORIUM_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFleetVehicle_sameAddress_returns400() throws Exception {
        doThrow(new SameDepartureAndDestinationException("mesmo endereço"))
                .when(vehicleService).createReservation(any(), any());

        mvc.perform(post("/api/reservation/fleet-vehicle")
                        .contentType(APPLICATION_JSON)
                        .content(VEHICLE_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMeetingRoomById_notFound_returns404() throws Exception {
        given(meetingRoomService.getReservationFromId(anyLong()))
                .willThrow(new ReservationIdIsNotFoundedException("não encontrada"));

        mvc.perform(get("/api/reservation/meeting-room/{id}", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMeetingRoomById_returns200() throws Exception {
        MeetingRoomResponseDTO dto = new MeetingRoomResponseDTO(
                1L, DATE, null, new RoomResponseDTO(2L, "2 ANDAR", true), ReserveType.MEETING_ROOM, null, LocalTime.of(9, 0),
                null, "Fulano", "TI", null, LocalTime.of(10, 0), 5, Boolean.TRUE, "Coffee para 5",
                null, null, "obs", ReserveStatus.ENVIADA_PARA_ANALISE, null, 7L, "1140028922", null, null);
        given(meetingRoomService.getReservationFromId(1L)).willReturn(dto);

        mvc.perform(get("/api/reservation/meeting-room/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.room.id").value(2))
                .andExpect(jsonPath("$.requesterDepartment").value("TI"))
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.telephone").value("1140028922"));
    }

    @Test
    void getAllMeetingRoom_returns200() throws Exception {
        mvc.perform(get("/api/reservation/meeting-room"))
                .andExpect(status().isOk());
    }

    @Test
    void updateMeetingRoom_returns200() throws Exception {
        mvc.perform(put("/api/reservation/meeting-room/{id}", 1)
                        .contentType(APPLICATION_JSON)
                        .content(MEETING_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void deleteMeetingRoom_returns204() throws Exception {
        mvc.perform(delete("/api/reservation/meeting-room/{id}", 1))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMeetingRoom_notFound_returns404() throws Exception {
        doThrow(new ReservationIdIsNotFoundedException("não encontrada"))
                .when(meetingRoomService).deleteReservation(anyLong());

        mvc.perform(delete("/api/reservation/meeting-room/{id}", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveAuditorium_returns200() throws Exception {
        mvc.perform(post("/api/reservation/auditorium/{id}/approve", 1))
                .andExpect(status().isOk());

        verify(auditoriumService).approve(eq(1L), any());
    }

    @Test
    void approveFleetVehicle_returns200() throws Exception {
        mvc.perform(post("/api/reservation/fleet-vehicle/{id}/approve", 1))
                .andExpect(status().isOk());

        verify(vehicleService).approve(eq(1L), any());
    }

    @Test
    void approveMeetingRoom_returns200() throws Exception {
        mvc.perform(post("/api/reservation/meeting-room/{id}/approve", 1))
                .andExpect(status().isOk());

        verify(meetingRoomService).approve(eq(1L), any());
    }

    @Test
    void rejectAuditorium_returns200() throws Exception {
        mvc.perform(post("/api/reservation/auditorium/{id}/reject", 1)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reasonFailure\":\"Sala indisponível\"}"))
                .andExpect(status().isOk());

        verify(auditoriumService).reject(eq(1L), eq(Map.of("reasonFailure", "Sala indisponível")), any());
    }

    @Test
    void rejectFleetVehicle_returns200() throws Exception {
        mvc.perform(post("/api/reservation/fleet-vehicle/{id}/reject", 1)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reasonFailure\":\"Frota em manutenção\"}"))
                .andExpect(status().isOk());

        verify(vehicleService).reject(eq(1L), eq(Map.of("reasonFailure", "Frota em manutenção")), any());
    }

    @Test
    void rejectMeetingRoom_returns200() throws Exception {
        mvc.perform(post("/api/reservation/meeting-room/{id}/reject", 1)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reasonFailure\":\"Conflito de agenda\"}"))
                .andExpect(status().isOk());

        verify(meetingRoomService).reject(eq(1L), eq(Map.of("reasonFailure", "Conflito de agenda")), any());
    }

    private static final String CANCEL_JSON = """
            {"id": 1, "reasonCancellation": "Evento adiado"}
            """;

    @Test
    void cancellAuditorium_returns200() throws Exception {
        mvc.perform(post("/api/reservation/auditorium/cancel")
                        .contentType(APPLICATION_JSON)
                        .content(CANCEL_JSON))
                .andExpect(status().isOk());

        verify(auditoriumService).cancelReservation(eq(new CancellationDTO(1L, "Evento adiado")), any());
    }

    @Test
    void cancellAuditorium_motivoChegaNoService() throws Exception {
        mvc.perform(post("/api/reservation/auditorium/cancel")
                        .contentType(APPLICATION_JSON)
                        .content(CANCEL_JSON))
                .andExpect(status().isOk());

        ArgumentCaptor<CancellationDTO> captor = ArgumentCaptor.forClass(CancellationDTO.class);
        verify(auditoriumService).cancelReservation(captor.capture(), any());

        assertEquals(1L, captor.getValue().id());
        assertEquals("Evento adiado", captor.getValue().reasonCancellation());
    }

    @Test
    void cancellAuditorium_canceledByNoCorpo_eIgnorado() throws Exception {
        String comAutorForjado = """
                {"id": 1, "reasonCancellation": "Evento adiado", "canceledBy": 999}
                """;

        mvc.perform(post("/api/reservation/auditorium/cancel")
                        .contentType(APPLICATION_JSON)
                        .content(comAutorForjado))
                .andExpect(status().isOk());

        ArgumentCaptor<CancellationDTO> captor = ArgumentCaptor.forClass(CancellationDTO.class);
        verify(auditoriumService).cancelReservation(captor.capture(), any());

        assertEquals(new CancellationDTO(1L, "Evento adiado"), captor.getValue());
        assertEquals(2, CancellationDTO.class.getRecordComponents().length,
                "CancellationDTO ganhou campo novo: se for o autor do cancelamento, ele passou a vir do cliente");
    }

    @Test
    void cancellAuditorium_gravaLogDeCancelamento() throws Exception {
        mvc.perform(post("/api/reservation/auditorium/cancel")
                        .contentType(APPLICATION_JSON)
                        .content(CANCEL_JSON))
                .andExpect(status().isOk());

        verify(logSystemService).log(
                eq("Cancelamento de reserva"), eq("Auditorio"), eq(ReserveType.AUDITORIUM),
                eq(1L), eq(LogsType.DELETE), any());
    }

    @Test
    void cancellAuditorium_reservaJaCancelada_returns400() throws Exception {
        doThrow(new ReservationWasCancell("A reserva já foi cancelada"))
                .when(auditoriumService).cancelReservation(any(), any());

        mvc.perform(post("/api/reservation/auditorium/cancel")
                        .contentType(APPLICATION_JSON)
                        .content(CANCEL_JSON))
                .andExpect(status().isBadRequest());

        verify(logSystemService, never()).log(any(), any(), any(), anyLong(), any(), any());
    }

    @Test
    void cancellAuditorium_semPermissao_returns403() throws Exception {
        doThrow(new SecurityException("Você não é o solicitante dessa reserva"))
                .when(auditoriumService).cancelReservation(any(), any());

        mvc.perform(post("/api/reservation/auditorium/cancel")
                        .contentType(APPLICATION_JSON)
                        .content(CANCEL_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancellAuditorium_reservaInexistente_returns404() throws Exception {
        doThrow(new ReservationIdIsNotFoundedException("Não foi encontrado a reserva"))
                .when(auditoriumService).cancelReservation(any(), any());

        mvc.perform(post("/api/reservation/auditorium/cancel")
                        .contentType(APPLICATION_JSON)
                        .content("{\"id\": 999, \"reasonCancellation\": \"Evento adiado\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelFleetVehicle_returns200() throws Exception {
        mvc.perform(post("/api/reservation/fleet-vehicle/cancel")
                        .contentType(APPLICATION_JSON)
                        .content(CANCEL_JSON))
                .andExpect(status().isOk());

        verify(vehicleService).cancelReservation(eq(new CancellationDTO(1L, "Evento adiado")), any());
    }

    @Test
    void cancelMeetingRoom_returns200() throws Exception {
        mvc.perform(post("/api/reservation/meeting-room/cancel")
                        .contentType(APPLICATION_JSON)
                        .content(CANCEL_JSON))
                .andExpect(status().isOk());

        verify(meetingRoomService).cancelReservation(eq(new CancellationDTO(1L, "Evento adiado")), any());
    }
}
