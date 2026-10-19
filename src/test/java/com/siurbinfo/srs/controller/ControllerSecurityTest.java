package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.driver.DriverController;
import com.siurbinfo.srs.controller.log.LogsController;
import com.siurbinfo.srs.controller.reservation.AuditoriumController;
import com.siurbinfo.srs.controller.reservation.FleetVehicleController;
import com.siurbinfo.srs.controller.room.RoomController;
import com.siurbinfo.srs.dto.driver.DriverResponseDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumResponseDTO;
import com.siurbinfo.srs.service.driver.DriverService;
import com.siurbinfo.srs.service.log.LogSystemService;
import com.siurbinfo.srs.service.reservation.AuditoriumService;
import com.siurbinfo.srs.service.reservation.VehicleService;
import com.siurbinfo.srs.service.room.RoomService;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
        AuditoriumController.class,
        FleetVehicleController.class,
        DriverController.class,
        RoomController.class,
        LogsController.class
})
@Import(ControllerSecurityTest.SecurityTestConfig.class)
@ActiveProfiles("test")
class ControllerSecurityTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean AuditoriumService auditoriumService;
    @MockitoBean VehicleService vehicleService;
    @MockitoBean DriverService driverService;
    @MockitoBean LogSystemService logSystemService;
    @MockitoBean RoomService roomService;

    @MockitoBean JwtDecoder jwtDecoder;

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

    private static RequestPostProcessor comGrupo(String grupo) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + grupo));
    }

    private static final String AUDITORIUM_JSON = String.format("""
            {
              "reservationDate": "%s",
              "timeOpenAuditorium": "17:30:00",
              "reason": "Palestra",
              "startTime": "18:00:00",
              "endTime": "19:00:00",
              "userNameRequester": "Fulano",
              "requesterDepartment": "TI",
              "numPeople": 50,
              "externalPublic": false,
              "necessaryScreen": true,
              "necessarySoundSystem": true,
              "necessaryCoffee": false
            }""", LocalDate.now());

    private static final String DRIVER_JSON = """
            {"name":"Joao Motorista","img":"foto.png"}""";

    @Test
    void aprovarAuditorio_comDivisaoAdministrativa_permite() throws Exception {
        mvc.perform(post("/api/reservation/auditorium/{id}/approve", 1)
                        .with(comGrupo("siurb-divisao-administrativa")))
                .andExpect(status().isOk());
    }

    @Test
    void aprovarAuditorio_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(post("/api/reservation/auditorium/{id}/approve", 1)
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());
    }

    @Test
    void aprovarFrota_comDivisaoFrotas_permite() throws Exception {
        mvc.perform(post("/api/reservation/fleet-vehicle/{id}/approve", 1)
                        .with(comGrupo("siurb-divisao-frotas")))
                .andExpect(status().isOk());
    }

    @Test
    void aprovarFrota_comDivisaoAdministrativa_barra403() throws Exception {
        mvc.perform(post("/api/reservation/fleet-vehicle/{id}/approve", 1)
                        .with(comGrupo("siurb-divisao-administrativa")))
                .andExpect(status().isForbidden());
    }

    @Test
    void aprovarFrota_comAdministrador_permite() throws Exception {
        mvc.perform(post("/api/reservation/fleet-vehicle/{id}/approve", 1)
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk());
    }

    @Test
    void criarMotorista_comDivisaoFrotas_permite() throws Exception {
        given(driverService.create(any()))
                .willReturn(new DriverResponseDTO(1L, "João Motorista", "foto.png"));

        mvc.perform(post("/api/driver")
                        .with(comGrupo("siurb-divisao-frotas"))
                        .contentType(APPLICATION_JSON)
                        .content(DRIVER_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void criarMotorista_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(post("/api/driver")
                        .with(comGrupo("siurb-usuario-geral"))
                        .contentType(APPLICATION_JSON)
                        .content(DRIVER_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void lerLogs_comAdministrador_permite() throws Exception {
        mvc.perform(get("/api/logs")
                        .param("offset", "1")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk());
    }

    @Test
    void lerLogs_comDivisaoAdministrativa_barra403() throws Exception {
        mvc.perform(get("/api/logs")
                        .param("offset", "1")
                        .with(comGrupo("siurb-divisao-administrativa")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ocultarSala_comAdministrador_permite() throws Exception {
        mvc.perform(patch("/api/room")
                        .param("id", "1")
                        .param("isActive", "false")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk());
    }

    @Test
    void ocultarSala_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(patch("/api/room")
                        .param("id", "1")
                        .param("isActive", "false")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarSalas_comUsuarioGeral_permite() throws Exception {
        mvc.perform(get("/api/room").with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk());
    }

    @Test
    void criarReservaAuditorio_comUsuarioGeral_permite() throws Exception {
        given(auditoriumService.createReservation(any(), any()))
                .willReturn(AuditoriumResponseDTO.builder().id(1L).build());

        mvc.perform(post("/api/reservation/auditorium")
                        .with(comGrupo("siurb-usuario-geral"))
                        .contentType(APPLICATION_JSON)
                        .content(AUDITORIUM_JSON))
                .andExpect(status().isCreated());
    }
}
