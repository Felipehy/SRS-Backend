package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.department.DepartmentController;
import com.siurbinfo.srs.dto.department.DepartmentRequestDTO;
import com.siurbinfo.srs.dto.department.DepartmentResponseDTO;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.exception.DepartmentIdNotFoundedException;
import com.siurbinfo.srs.exception.DepartmentInUseException;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import com.siurbinfo.srs.service.department.DepartmentService;
import com.siurbinfo.srs.service.log.LogSystemService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepartmentController.class)
@Import(DepartmentControllerTest.SecurityTestConfig.class)
@ActiveProfiles("test")
class DepartmentControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DepartmentService service;

    @MockitoBean
    CognitoUserService cognitoUserService;

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

    private static final String DEPARTMENT_JSON = """
            {
              "name": "Tecnologia",
              "idRoom": 3
            }""";

    @Test
    void getAllDepartment_comAdministrador_devolveLista() throws Exception {
        given(service.getAllDepartment()).willReturn(List.of(
                new DepartmentResponseDTO("Tecnologia", "3 ANDAR", 3L, 1L),
                new DepartmentResponseDTO("Juridico", "5 ANDAR", 5L, 2L)));

        mvc.perform(get("/api/department").with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Tecnologia"))
                .andExpect(jsonPath("$[0].room").value("3 ANDAR"))
                .andExpect(jsonPath("$[0].idRoom").value(3))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].name").value("Juridico"));
    }

    @Test
    void getAllDepartment_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(get("/api/department").with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());

        verify(service, never()).getAllDepartment();
    }

    @Test
    void createDepartment_comAdministrador_criaERegistraLog() throws Exception {
        given(service.createDepartment(any(DepartmentRequestDTO.class)))
                .willReturn(new DepartmentResponseDTO("Tecnologia", "3 ANDAR", 3L, 1L));

        mvc.perform(post("/api/department")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content(DEPARTMENT_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tecnologia"))
                .andExpect(jsonPath("$.id").value(1));

        verify(service).createDepartment(eq(new DepartmentRequestDTO("Tecnologia", 3L)));
        verify(logSystemService).log(eq("Criação de um novo departamento"), eq("Admin"),
                eq(null), eq(1L), eq(LogsType.CREATE), any(Jwt.class));
    }

    @Test
    void createDepartment_semNome_barra400() throws Exception {
        mvc.perform(post("/api/department")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "  ", "idRoom": 3}"""))
                .andExpect(status().isBadRequest());

        verify(service, never()).createDepartment(any());
    }

    @Test
    void createDepartment_semIdRoom_barra400() throws Exception {
        mvc.perform(post("/api/department")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"name": "Tecnologia"}"""))
                .andExpect(status().isBadRequest());

        verify(service, never()).createDepartment(any());
    }

    @Test
    void createDepartment_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(post("/api/department")
                        .with(comGrupo("siurb-usuario-geral"))
                        .contentType(APPLICATION_JSON)
                        .content(DEPARTMENT_JSON))
                .andExpect(status().isForbidden());

        verify(service, never()).createDepartment(any());
    }

    @Test
    void updateDepartment_comAdministrador_atualizaERegistraLog() throws Exception {
        mvc.perform(patch("/api/department")
                        .param("id", "7")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content(DEPARTMENT_JSON))
                .andExpect(status().isOk());

        verify(service).updateDepartment(eq(new DepartmentRequestDTO("Tecnologia", 3L)), eq(7L));
        verify(logSystemService).log(eq("Alteração no departamento"), eq("Admin"),
                eq(null), eq(7L), eq(LogsType.UPDATE), any(Jwt.class));
    }

    @Test
    void updateDepartment_semParametroId_barra400() throws Exception {
        mvc.perform(patch("/api/department")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content(DEPARTMENT_JSON))
                .andExpect(status().isBadRequest());

        verify(service, never()).updateDepartment(any(), any());
    }

    @Test
    void updateDepartment_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(patch("/api/department")
                        .param("id", "7")
                        .with(comGrupo("siurb-usuario-geral"))
                        .contentType(APPLICATION_JSON)
                        .content(DEPARTMENT_JSON))
                .andExpect(status().isForbidden());

        verify(service, never()).updateDepartment(any(), any());
    }

    @Test
    void deleteDepartment_comAdministrador_devolve204ERegistraLog() throws Exception {
        mvc.perform(delete("/api/department")
                        .param("id", "7")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isNoContent());

        verify(service).deleteDepartment(7L);
        verify(logSystemService).log(eq("Exclusão de um departamento"), eq("Admin"),
                eq(null), eq(7L), eq(LogsType.DELETE), any(Jwt.class));
    }

    @Test
    void deleteDepartment_idInexistente_devolve404() throws Exception {
        willThrow(new DepartmentIdNotFoundedException("Não foi encontrado o departamento: 99"))
                .given(service).deleteDepartment(99L);

        mvc.perform(delete("/api/department")
                        .param("id", "99")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Departamento não encontrado"));

        verify(logSystemService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deleteDepartment_comUsuariosVinculados_devolve409() throws Exception {
        willThrow(new DepartmentInUseException("O departamento ainda possui 3 usuário(s) vinculado(s)"))
                .given(service).deleteDepartment(7L);

        mvc.perform(delete("/api/department")
                        .param("id", "7")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Departamento em uso"))
                .andExpect(jsonPath("$.detail").value("O departamento ainda possui 3 usuário(s) vinculado(s)"));

        verify(logSystemService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateDepartment_idInexistente_devolve404() throws Exception {
        willThrow(new DepartmentIdNotFoundedException("Não foi encontrado o departamento: 99"))
                .given(service).updateDepartment(any(DepartmentRequestDTO.class), eq(99L));

        mvc.perform(patch("/api/department")
                        .param("id", "99")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content(DEPARTMENT_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDepartment_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(delete("/api/department")
                        .param("id", "7")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());

        verify(service, never()).deleteDepartment(any());
    }

    @Test
    void getDepartmentByUserId_devolveODepartamento() throws Exception {
        given(cognitoUserService.getDepartmentFromUser(eq(7L), any(Jwt.class)))
                .willReturn(new DepartmentResponseDTO("Tecnologia", "3 ANDAR", 3L, 7L));

        mvc.perform(get("/api/department/{id}", 7).with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tecnologia"))
                .andExpect(jsonPath("$.room").value("3 ANDAR"))
                .andExpect(jsonPath("$.idRoom").value(3))
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void getDepartmentByUserId_naoExigeMaisGrupoAdministrador() throws Exception {
        given(cognitoUserService.getDepartmentFromUser(eq(7L), any(Jwt.class)))
                .willReturn(new DepartmentResponseDTO("Tecnologia", "3 ANDAR", 3L, 7L));

        mvc.perform(get("/api/department/{id}", 7).with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk());

        verify(cognitoUserService).getDepartmentFromUser(eq(7L), any(Jwt.class));
    }

    @Test
    void getDepartmentByUserId_repassaOJwtAutenticadoProService() throws Exception {
        given(cognitoUserService.getDepartmentFromUser(eq(7L), any(Jwt.class)))
                .willReturn(new DepartmentResponseDTO("Tecnologia", "3 ANDAR", 3L, 7L));

        mvc.perform(get("/api/department/{id}", 7).with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isOk());

        ArgumentCaptor<Jwt> captor = ArgumentCaptor.forClass(Jwt.class);
        verify(cognitoUserService).getDepartmentFromUser(eq(7L), captor.capture());
        assertNotNull(captor.getValue());
    }

    @Test
    void getDepartmentByUserId_idDeOutroUsuario_devolve403() throws Exception {
        willThrow(new SecurityException("É necessario estar logado na conta do usuário para pegar seu departmento"))
                .given(cognitoUserService).getDepartmentFromUser(eq(99L), any(Jwt.class));

        mvc.perform(get("/api/department/{id}", 99).with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDepartmentByUserId_usuarioSemDepartamento_devolve404() throws Exception {
        willThrow(new DepartmentIdNotFoundedException("Usuário não possui departamento vinculado"))
                .given(cognitoUserService).getDepartmentFromUser(eq(7L), any(Jwt.class));

        mvc.perform(get("/api/department/{id}", 7).with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isNotFound());
    }
}
