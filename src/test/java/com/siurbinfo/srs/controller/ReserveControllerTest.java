package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.reservation.AuditoriumController;
import com.siurbinfo.srs.controller.reservation.FleetVehicleController;
import com.siurbinfo.srs.controller.reservation.MeetingRoomController;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomResponseDTO;
import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.SameDepartureAndDestinationException;
import com.siurbinfo.srs.exception.StartTimeIsNotBeforeEndTimeException;
import com.siurbinfo.srs.service.reservation.AuditoriumService;
import com.siurbinfo.srs.service.reservation.MeetingRoomService;
import com.siurbinfo.srs.service.reservation.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuditoriumController.class, FleetVehicleController.class, MeetingRoomController.class})
@AutoConfigureMockMvc(addFilters = false)
class ReserveControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuditoriumService auditoriumService;

    @MockitoBean
    VehicleService vehicleService;

    @MockitoBean
    MeetingRoomService meetingRoomService;

    // JwtDecoder mockado só pra o auto-config do OAuth2 não tentar resolver o issuer-uri via rede
    @MockitoBean
    JwtDecoder jwtDecoder;

    private static final LocalDate DATE = LocalDate.now();

    private static final String MEETING_JSON = String.format("""
            {
              "reservationDate": "%s",
              "roomId": 1,
              "startTime": "09:00:00",
              "userNameRequester": "Fulano",
              "endTime": "10:00:00",
              "numPeople": 5,
              "isCoffee": true,
              "observation": "obs"
            }""",DATE);

    private static final String AUDITORIUM_JSON = String.format("""
            {
              "reservationDate": "%s",
              "timeOpenAuditorium": "08:30:00",
              "reason": "Palestra",
              "startTime": "09:00:00",
              "endTime": "11:00:00",
              "userNameRequester": "Fulano",
              "numPeople": 50,
              "externalPublic": false,
              "necessaryScreen": true,
              "necessarySoundSystem": true,
              "necessaryCoffee": false
            }""", DATE);

    private static final String VEHICLE_JSON = String.format("""
            {
              "reservationDate": "%s",
              "exitTime": "08:00:00",
              "reason": "Visita",
              "departureAddress": "Sede",
              "destinationAddress": "Cliente X",
              "necessaryReturn": true,
              "returnTime": "18:00:00",
              "departureAddressReturn": "Cliente X",
              "destinationAddressReturn": "Sede",
              "contact": "11999998888",
              "userNameRequester": "Fulano",
              "necessaryVan": false,
              "reasonUseVan": null,
              "typeVehicle": "CARRO",
              "quantityPassengers": 3
            }""", DATE);

    // ---------- POST (201) ----------

    @Test
    void createMeetingRoom_returns201() throws Exception {
        mvc.perform(post("/api/reservation/meeting-room")
                        .contentType(APPLICATION_JSON)
                        .content(MEETING_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void createAuditorium_returns201() throws Exception {
        mvc.perform(post("/api/reservation/auditorium")
                        .contentType(APPLICATION_JSON)
                        .content(AUDITORIUM_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void createFleetVehicle_returns201() throws Exception {
        mvc.perform(post("/api/reservation/fleet-vehicle")
                        .contentType(APPLICATION_JSON)
                        .content(VEHICLE_JSON))
                .andExpect(status().isCreated());
    }

    // ---------- POST validação de body (400) ----------

    @Test
    void createMeetingRoom_invalidBody_returns400() throws Exception {
        mvc.perform(post("/api/reservation/meeting-room")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Mapeamento de exceções do GlobalExceptionHandler ----------

    @Test
    void createMeetingRoom_conflict_returns409() throws Exception {
        doThrow(new ReservationConflictException("conflito"))
                .when(meetingRoomService).createReservation(any());

        mvc.perform(post("/api/reservation/meeting-room")
                        .contentType(APPLICATION_JSON)
                        .content(MEETING_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void createAuditorium_invalidTime_returns400() throws Exception {
        doThrow(new StartTimeIsNotBeforeEndTimeException("tempo inválido"))
                .when(auditoriumService).createReservation(any());

        mvc.perform(post("/api/reservation/auditorium")
                        .contentType(APPLICATION_JSON)
                        .content(AUDITORIUM_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFleetVehicle_sameAddress_returns400() throws Exception {
        doThrow(new SameDepartureAndDestinationException("mesmo endereço"))
                .when(vehicleService).createReservation(any());

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

    // ---------- GET (200) ----------

    @Test
    void getMeetingRoomById_returns200() throws Exception {
        MeetingRoomResponseDTO dto = new MeetingRoomResponseDTO(
                1L, DATE, null, new RoomResponseDTO(2L, "2 ANDAR"), ReserveType.MEETING_ROOM, null, LocalTime.of(9, 0),
                "Fulano", null, LocalTime.of(10, 0), 5, Boolean.TRUE, "obs",
                ReserveStatus.ENVIADA_PARA_ANALISE, null);
        given(meetingRoomService.getReservationFromId(1L)).willReturn(dto);

        mvc.perform(get("/api/reservation/meeting-room/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.room.id").value(2));
    }

    @Test
    void getAllMeetingRoom_returns200() throws Exception {
        mvc.perform(get("/api/reservation/meeting-room"))
                .andExpect(status().isOk());
    }

    // ---------- PUT (200) / DELETE (204) ----------

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

    // ---------- APPROVE (200) ----------

    @Test
    void approveAuditorium_returns200() throws Exception {
        mvc.perform(post("/api/reservation/auditorium/{id}/approve", 1))
                .andExpect(status().isOk());

        verify(auditoriumService).approve(1L);
    }

    @Test
    void approveFleetVehicle_returns200() throws Exception {
        mvc.perform(post("/api/reservation/fleet-vehicle/{id}/approve", 1))
                .andExpect(status().isOk());

        verify(vehicleService).approve(1L);
    }

    @Test
    void approveMeetingRoom_returns200() throws Exception {
        mvc.perform(post("/api/reservation/meeting-room/{id}/approve", 1))
                .andExpect(status().isOk());

        verify(meetingRoomService).approve(1L);
    }

    // ---------- REJECT (200) ----------

    @Test
    void rejectAuditorium_returns200() throws Exception {
        mvc.perform(post("/api/reservation/auditorium/{id}/reject", 1)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reasonFailure\":\"Sala indisponível\"}"))
                .andExpect(status().isOk());

        verify(auditoriumService).reject(1L, Map.of("reasonFailure", "Sala indisponível"));
    }

    @Test
    void rejectFleetVehicle_returns200() throws Exception {
        mvc.perform(post("/api/reservation/fleet-vehicle/{id}/reject", 1)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reasonFailure\":\"Frota em manutenção\"}"))
                .andExpect(status().isOk());

        verify(vehicleService).reject(1L, Map.of("reasonFailure", "Frota em manutenção"));
    }

    @Test
    void rejectMeetingRoom_returns200() throws Exception {
        mvc.perform(post("/api/reservation/meeting-room/{id}/reject", 1)
                        .contentType(APPLICATION_JSON)
                        .content("{\"reasonFailure\":\"Conflito de agenda\"}"))
                .andExpect(status().isOk());

        verify(meetingRoomService).reject(1L, Map.of("reasonFailure", "Conflito de agenda"));
    }
}
