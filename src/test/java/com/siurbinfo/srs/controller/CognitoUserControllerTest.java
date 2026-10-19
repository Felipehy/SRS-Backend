package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.cognito.CognitoUserController;
import com.siurbinfo.srs.dto.cognito.group.GroupResponse;
import com.siurbinfo.srs.dto.cognito.user.DeleteUserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.dto.reservation.ReservationsDTO;
import com.siurbinfo.srs.dto.user.ColorsRequestDTO;
import com.siurbinfo.srs.dto.user.ListReservationsUserDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.exception.AttributeIsEmptyException;
import com.siurbinfo.srs.service.cognito.user.CognitoUserServiceImpl;
import com.siurbinfo.srs.service.log.LogSystemService;
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
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CognitoUserController.class)
@Import(CognitoUserControllerTest.SecurityTestConfig.class)
@ActiveProfiles("test")
class CognitoUserControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CognitoUserServiceImpl userService;

    @MockitoBean
    LogSystemService logSystemService;

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

    private static RequestPostProcessor comGrupo(String grupo) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + grupo));
    }

    private static final String COLORS_JSON = """
            {
              "colorAuditorium": "#111111",
              "colorFleetVehicles": "#222222",
              "colorMeetingRoom": "#333333"
            }""";

    @Test
    void getUser_devolveNomeCoresEGrupos() throws Exception {
        given(userService.getUser(any(Jwt.class))).willReturn(new UserResponseDTO(
                1L, "fulano@teste.com", "Fulano", "de Tal",
                Map.of("colorAuditorium", "#111111",
                        "colorFleetVehicle", "#222222",
                        "colorMeetingRoom", "#333333"),
                false,
                List.of(new GroupResponse("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos")),
                7L, true));

        mvc.perform(get("/api/user").with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.givenName").value("Fulano"))
                .andExpect(jsonPath("$.familyName").value("de Tal"))
                .andExpect(jsonPath("$.colors.colorAuditorium").value("#111111"))
                .andExpect(jsonPath("$.colors.colorFleetVehicle").value("#222222"))
                .andExpect(jsonPath("$.colors.colorMeetingRoom").value("#333333"))
                .andExpect(jsonPath("$.groups", hasSize(1)))
                .andExpect(jsonPath("$.groups[0].groupName").value("Frota"))
                .andExpect(jsonPath("$.groups[0].groupCognito").value("siurb-frota"))
                .andExpect(jsonPath("$.groups[0].colorGroup").value("#ff0000"))
                .andExpect(jsonPath("$.groups[0].description").value("Reserva de veiculos"))
                .andExpect(jsonPath("$.idDepartment").value(7))
                .andExpect(jsonPath("$.isFirstLogin").value(true));
    }

    @Test
    void getUser_semGrupos_devolveListaVaziaNoJson() throws Exception {
        given(userService.getUser(any(Jwt.class))).willReturn(new UserResponseDTO(
                1L, "fulano@teste.com", "Fulano", "de Tal", Map.of(), false, List.of(), null, false));

        mvc.perform(get("/api/user").with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups", hasSize(0)));
    }

    @Test
    void getReservations_devolvePainelEmJson() throws Exception {
        given(userService.getReservationsByUser(any(Jwt.class), isNull(), eq(0L), isNull())).willReturn(
                new ReservationsDTO(12L, List.of(new ListReservationsUserDTO(
                        1L, "AUDITORIUM", "APROVADA", LocalDate.of(2026, 8, 1),
                        LocalTime.of(9, 0), LocalTime.of(11, 0), "Fulano de Tal", 7L))));

        mvc.perform(get("/api/user/reservations")
                        .param("pagination", "0")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numReservations").value(12))
                .andExpect(jsonPath("$.reservations", hasSize(1)))
                .andExpect(jsonPath("$.reservations[0].id").value(1))
                .andExpect(jsonPath("$.reservations[0].reservationType").value("AUDITORIUM"))
                .andExpect(jsonPath("$.reservations[0].status").value("APROVADA"))
                .andExpect(jsonPath("$.reservations[0].reservationDate").value("2026-08-01"))
                .andExpect(jsonPath("$.reservations[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.reservations[0].endTime").value("11:00:00"))
                .andExpect(jsonPath("$.reservations[0].idUser").value(7));
    }

    @Test
    void getReservations_repassaDataEPagina() throws Exception {
        given(userService.getReservationsByUser(any(Jwt.class), any(), any(), any()))
                .willReturn(new ReservationsDTO(0L, List.of()));

        mvc.perform(get("/api/user/reservations")
                        .param("pagination", "2")
                        .param("date", "2026-08-01")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk());

        verify(userService).getReservationsByUser(any(Jwt.class), eq(LocalDate.of(2026, 8, 1)), eq(2L), isNull());
    }

    @Test
    void getReservations_repassaFiltroDeTipos() throws Exception {
        given(userService.getReservationsByUser(any(Jwt.class), any(), any(), any()))
                .willReturn(new ReservationsDTO(0L, List.of()));

        mvc.perform(get("/api/user/reservations")
                        .param("pagination", "0")
                        .param("type", "AUDITORIUM", "MEETING_ROOM")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/user/reservations")
                        .param("pagination", "0")
                        .param("type", "AUDITORIUM,MEETING_ROOM")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk());

        verify(userService, times(2)).getReservationsByUser(any(Jwt.class), isNull(), eq(0L),
                eq(List.of("AUDITORIUM", "MEETING_ROOM")));
    }

    @Test
    void finishTutorial_usuarioAutenticado_retorna200() throws Exception {
        mvc.perform(patch("/api/user/tutorial").with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk());

        verify(userService).finishTutorial(any(Jwt.class));
    }

    @Test
    void getReservations_semPagination_retorna400() throws Exception {
        mvc.perform(get("/api/user/reservations").with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getReservationsByUser(any(), any(), any(), any());
    }

    @Test
    void getReservations_paginacaoNegativa_retorna400() throws Exception {
        mvc.perform(get("/api/user/reservations")
                        .param("pagination", "-1")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetros inválidos"))
                .andExpect(jsonPath("$.errors.*", hasItem("A página não pode ser negativa")));

        verify(userService, never()).getReservationsByUser(any(), any(), any(), any());
    }

    @Test
    void getReservations_paginaZero_eAceita() throws Exception {
        given(userService.getReservationsByUser(any(Jwt.class), any(), any(), any()))
                .willReturn(new ReservationsDTO(0L, List.of()));

        mvc.perform(get("/api/user/reservations")
                        .param("pagination", "0")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numReservations").value(0))
                .andExpect(jsonPath("$.reservations", hasSize(0)));
    }

    @Test
    void getReservations_dataMalFormada_retorna400() throws Exception {
        mvc.perform(get("/api/user/reservations")
                        .param("pagination", "0")
                        .param("date", "01/08/2026")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isBadRequest());

        verify(userService, never()).getReservationsByUser(any(), any(), any(), any());
    }

    @Test
    void setColors_usuarioAutenticado_gravaERetorna200() throws Exception {
        mvc.perform(post("/api/user/color")
                        .with(comGrupo("siurb-usuario-geral"))
                        .contentType(APPLICATION_JSON)
                        .content(COLORS_JSON))
                .andExpect(status().isOk());

        verify(userService).setColors(any(Jwt.class),
                eq(new ColorsRequestDTO("#111111", "#222222", "#333333")));
    }

    @Test
    void setColors_corMaiorQueSeteCaracteres_barra400() throws Exception {
        String jsonInvalido = """
                {
                  "colorAuditorium": "#1111111",
                  "colorFleetVehicles": "#222222",
                  "colorMeetingRoom": "#333333"
                }""";

        mvc.perform(post("/api/user/color")
                        .with(comGrupo("siurb-usuario-geral"))
                        .contentType(APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest());

        verify(userService, never()).setColors(any(), any());
    }

    @Test
    void listUsers_comAdministrador_permite() throws Exception {
        mvc.perform(get("/api/admin/list-users").with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk());
    }

    @Test
    void listUsers_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(get("/api/admin/list-users").with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());
    }

    @Test
    void setDepartment_comAdministrador_gravaERetorna200() throws Exception {
        mvc.perform(post("/api/admin/department")
                        .param("id", "7")
                        .param("email", "fulano@prefeitura.sp.gov.br")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk());

        verify(userService).setDepartment(7L, "fulano@prefeitura.sp.gov.br");
    }

    @Test
    void setDepartment_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(post("/api/admin/department")
                        .param("id", "7")
                        .param("email", "fulano@prefeitura.sp.gov.br")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());

        verify(userService, never()).setDepartment(any(), any());
    }

    @Test
    void setDepartment_semTokenJwt_barra403ENaoGrava() throws Exception {
        mvc.perform(post("/api/admin/department")
                        .param("id", "7")
                        .param("email", "fulano@prefeitura.sp.gov.br")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_siurb-administrador"))))
                .andExpect(status().isForbidden());

        verify(userService, never()).setDepartment(any(), any());
    }

    private static final String USER_JSON = """
            {
              "givenName": "Fulano",
              "familyName": "de Tal",
              "email": "fulano@prefeitura.sp.gov.br",
              "idDepartment": 7,
              "phoneNumber": "11999998888"
            }""";

    private static final UserRequest USER_REQUEST = new UserRequest(
            "Fulano", "de Tal", "fulano@prefeitura.sp.gov.br", 7L, "11999998888");

    @Test
    void createUser_comAdministrador_criaDevolveAMensagemELoga() throws Exception {
        given(userService.createUser(any(UserRequest.class)))
                .willReturn(new UserResponseMessage(42L, "Conta criada com sucesso", "Fulano"));

        mvc.perform(post("/api/admin/user")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content(USER_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.message").value("Conta criada com sucesso"))
                .andExpect(jsonPath("$.given_name").value("Fulano"));

        verify(userService).createUser(USER_REQUEST);
        verify(logSystemService).log(eq("Criação de usuario"), eq("Admin"),
                eq(null), eq(42L), eq(LogsType.CREATE), any(Jwt.class));
    }

    @Test
    void createUser_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(post("/api/admin/user")
                        .with(comGrupo("siurb-usuario-geral"))
                        .contentType(APPLICATION_JSON)
                        .content(USER_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).createUser(any());
        verify(logSystemService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createUser_emailInvalido_barra400() throws Exception {
        mvc.perform(post("/api/admin/user")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content(USER_JSON.replace("fulano@prefeitura.sp.gov.br", "nao-e-email")))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any());
        verify(logSystemService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void createUser_telefoneComLetras_barra400() throws Exception {
        mvc.perform(post("/api/admin/user")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content(USER_JSON.replace("11999998888", "11-abc-8888")))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any());
    }

    @Test
    void updateUserAtt_comAdministrador_atualizaDevolveAMensagemELoga() throws Exception {
        given(userService.updateUserAtt(any(UserRequest.class)))
                .willReturn(new UserResponseMessage(42L, "Usuario atualizado com sucesso", "Fulano"));

        mvc.perform(patch("/api/admin/user")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content(USER_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.message").value("Usuario atualizado com sucesso"))
                .andExpect(jsonPath("$.given_name").value("Fulano"));

        verify(userService).updateUserAtt(USER_REQUEST);
        verify(logSystemService).log(eq("Alteração no usuario"), eq("Admin"),
                eq(null), eq(42L), eq(LogsType.UPDATE), any(Jwt.class));
    }

    @Test
    void updateUserAtt_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(patch("/api/admin/user")
                        .with(comGrupo("siurb-usuario-geral"))
                        .contentType(APPLICATION_JSON)
                        .content(USER_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUserAtt(any());
        verify(logSystemService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateUserAtt_semNadaParaAlterar_barra400ENaoLoga() throws Exception {
        given(userService.updateUserAtt(any(UserRequest.class)))
                .willThrow(new AttributeIsEmptyException("Campos vazios ou informações parecidas"));

        mvc.perform(patch("/api/admin/user")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content(USER_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro nos atributos"));

        verify(logSystemService, never()).log(any(), any(), any(), any(), any(), any());
    }

    private static final String DELETE_USER_JSON = """
            {"email": "fulano@prefeitura.sp.gov.br"}""";

    @Test
    void deleteUser_comAdministrador_excluiELogaAExclusaoComOEmail() throws Exception {
        given(userService.deleteUser(any(DeleteUserRequest.class)))
                .willReturn(new UserResponseMessage(42L, "Pessoa deletada com sucesso!", null));

        mvc.perform(delete("/api/admin/user")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content(DELETE_USER_JSON))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(new DeleteUserRequest("fulano@prefeitura.sp.gov.br"));
        verify(logSystemService).log(eq("Usuario deletado, email: fulano@prefeitura.sp.gov.br"), eq("Admin"),
                eq(null), eq(42L), eq(LogsType.DELETE), any(Jwt.class));
    }

    @Test
    void deleteUser_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(delete("/api/admin/user")
                        .with(comGrupo("siurb-usuario-geral"))
                        .contentType(APPLICATION_JSON)
                        .content(DELETE_USER_JSON))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteUser(any());
        verify(logSystemService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteUser_emailInvalido_barra400() throws Exception {
        mvc.perform(delete("/api/admin/user")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email": "nao-e-email"}"""))
                .andExpect(status().isBadRequest());

        verify(userService, never()).deleteUser(any());
        verify(logSystemService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void resetPassword_comAdministrador_resetaELogaComOEmail() throws Exception {
        given(userService.resetPassword(eq("fulano@prefeitura.sp.gov.br"), any(Jwt.class)))
                .willReturn(new UserResponseMessage(42L, "Reset de senha", "fulano@prefeitura.sp.gov.br"));

        mvc.perform(post("/api/admin/reset-password")
                        .param("email", "fulano@prefeitura.sp.gov.br")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk());

        verify(logSystemService).log(eq("Reset de senha do usuario fulano@prefeitura.sp.gov.br"), eq("Admin"),
                eq(null), eq(42L), eq(LogsType.UPDATE), any(Jwt.class));
    }

    @Test
    void resetPassword_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(post("/api/admin/reset-password")
                        .param("email", "fulano@prefeitura.sp.gov.br")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());

        verify(userService, never()).resetPassword(any(), any());
    }

    @Test
    void resetMFA_comAdministrador_resetaELogaComOEmail() throws Exception {
        mvc.perform(post("/api/admin/reset-mfa")
                        .param("email", "fulano@prefeitura.sp.gov.br")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk());

        verify(userService).resetMFA("fulano@prefeitura.sp.gov.br");
        verify(logSystemService).log(eq("Reset do MFA do fulano@prefeitura.sp.gov.br"), eq("Admin"),
                eq(null), eq(null), eq(LogsType.UPDATE), any(Jwt.class));
    }

    @Test
    void resetMFA_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(post("/api/admin/reset-mfa")
                        .param("email", "fulano@prefeitura.sp.gov.br")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());

        verify(userService, never()).resetMFA(any());
        verify(logSystemService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void resetMFA_semEmail_retorna400() throws Exception {
        mvc.perform(post("/api/admin/reset-mfa").with(comGrupo("siurb-administrador")))
                .andExpect(status().isBadRequest());

        verify(userService, never()).resetMFA(any());
    }
}
