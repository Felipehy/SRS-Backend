package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.reservation.ReservationController;
import com.siurbinfo.srs.dto.reservation.ReservationsDTO;
import com.siurbinfo.srs.dto.user.ListReservationsUserDTO;
import com.siurbinfo.srs.exception.InvalidTimeException;
import com.siurbinfo.srs.service.reservation.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@Import(ReservationControllerTest.SecurityTestConfig.class)
@ActiveProfiles("test")
class ReservationControllerTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 8, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 9, 1);

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ReservationService service;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @TestConfiguration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityTestConfig {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
            return http.build();
        }
    }

    private static RequestPostProcessor usuario() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_siurb-usuario-geral"));
    }

    @Test
    void getAllReservations_devolveListaEmJson() throws Exception {
        given(service.getAllReservations(eq(INICIO), eq(FIM), any(Jwt.class))).willReturn(
                new ReservationsDTO(2L, List.of(
                        new ListReservationsUserDTO(1L, "AUDITORIUM", "APROVADA", INICIO,
                                LocalTime.of(9, 0), LocalTime.of(11, 0), "Fulano de Tal", 7L),
                        new ListReservationsUserDTO(5L, "FLEET_VEHICLE", "CANCELADA", INICIO,
                                LocalTime.of(8, 0), null, "Fulano de Tal", 7L))));

        mvc.perform(get("/api/reservation")
                        .param("startTime", "2026-08-01")
                        .param("endTime", "2026-09-01")
                        .with(usuario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numReservations").value(2))
                .andExpect(jsonPath("$.reservations", hasSize(2)))
                .andExpect(jsonPath("$.reservations[0].id").value(1))
                .andExpect(jsonPath("$.reservations[0].reservationType").value("AUDITORIUM"))
                .andExpect(jsonPath("$.reservations[0].status").value("APROVADA"))
                .andExpect(jsonPath("$.reservations[0].reservationDate").value("2026-08-01"))
                .andExpect(jsonPath("$.reservations[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.reservations[0].endTime").value("11:00:00"))
                .andExpect(jsonPath("$.reservations[0].userNameRequester").value("Fulano de Tal"))
                .andExpect(jsonPath("$.reservations[0].idUser").value(7))
                .andExpect(jsonPath("$.reservations[1].reservationType").value("FLEET_VEHICLE"))
                .andExpect(jsonPath("$.reservations[1].endTime").doesNotExist());
    }

    @Test
    void getAllReservations_repassaAsDatasEOTokenProService() throws Exception {
        given(service.getAllReservations(any(), any(), any())).willReturn(new ReservationsDTO(0L, List.of()));

        mvc.perform(get("/api/reservation")
                        .param("startTime", "2026-08-01")
                        .param("endTime", "2026-09-01")
                        .with(usuario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numReservations").value(0))
                .andExpect(jsonPath("$.reservations", hasSize(0)));

        verify(service).getAllReservations(eq(INICIO), eq(FIM), any(Jwt.class));
    }

    @Test
    void getAllReservations_semStartTime_retorna400() throws Exception {
        mvc.perform(get("/api/reservation")
                        .param("endTime", "2026-09-01")
                        .with(usuario()))
                .andExpect(status().isBadRequest());

        verify(service, never()).getAllReservations(any(), any(), any());
    }

    @Test
    void getAllReservations_semEndTime_retorna400() throws Exception {
        mvc.perform(get("/api/reservation")
                        .param("startTime", "2026-08-01")
                        .with(usuario()))
                .andExpect(status().isBadRequest());

        verify(service, never()).getAllReservations(any(), any(), any());
    }

    @Test
    void getAllReservations_dataMalFormada_retorna400() throws Exception {
        mvc.perform(get("/api/reservation")
                        .param("startTime", "01/08/2026")
                        .param("endTime", "2026-09-01")
                        .with(usuario()))
                .andExpect(status().isBadRequest());

        verify(service, never()).getAllReservations(any(), any(), any());
    }

    @Test
    void getAllReservations_erroDeHorarioNoService_retorna400() throws Exception {
        given(service.getAllReservations(any(), any(), any()))
                .willThrow(new InvalidTimeException("É necessário o tempo inicial"));

        mvc.perform(get("/api/reservation")
                        .param("startTime", "2026-08-01")
                        .param("endTime", "2026-09-01")
                        .with(usuario()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("É necessário o tempo inicial"));
    }
}
