package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.cognito.CognitoGroupController;
import com.siurbinfo.srs.dto.cognito.group.GroupRequest;
import com.siurbinfo.srs.dto.cognito.group.GroupResponse;
import com.siurbinfo.srs.dto.cognito.group.GroupResponseMessage;
import com.siurbinfo.srs.dto.cognito.group.ListUsersInGroupResponseDTO;
import com.siurbinfo.srs.dto.cognito.group.UserGroupsRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.exception.EmptyRequestException;
import com.siurbinfo.srs.service.cognito.group.CognitoGroupServiceImpl;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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

@WebMvcTest(CognitoGroupController.class)
@Import(CognitoGroupControllerTest.SecurityTestConfig.class)
@ActiveProfiles("test")
class CognitoGroupControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CognitoGroupServiceImpl serviceGroup;

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

    @Test
    void listGroup_comAdministrador_devolveGrupos() throws Exception {
        given(serviceGroup.listGroup()).willReturn(List.of(
                new GroupResponse("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos"),
                new GroupResponse("Auditorio", "siurb-auditorio", "#00ff00", "Reserva do auditorio")));

        mvc.perform(get("/api/admin/list-group").with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].groupName").value("Frota"))
                .andExpect(jsonPath("$[0].groupCognito").value("siurb-frota"))
                .andExpect(jsonPath("$[0].colorGroup").value("#ff0000"))
                .andExpect(jsonPath("$[0].description").value("Reserva de veiculos"))
                .andExpect(jsonPath("$[1].groupCognito").value("siurb-auditorio"));
    }

    @Test
    void listGroup_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(get("/api/admin/list-group").with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());

        verify(serviceGroup, never()).listGroup();
    }

    @Test
    void listGroupsForUser_devolveGruposDoUsuario() throws Exception {
        given(serviceGroup.listGroupsForUser(anyString()))
                .willReturn(List.of(
                        new GroupResponse("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos")));

        mvc.perform(post("/api/admin/user/groups")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email": "fulano@prefeitura.sp.gov.br"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupName").value("Frota"))
                .andExpect(jsonPath("$[0].groupCognito").value("siurb-frota"))
                .andExpect(jsonPath("$[0].description").value("Reserva de veiculos"));

        verify(serviceGroup).listGroupsForUser("fulano@prefeitura.sp.gov.br");
    }

    @Test
    void listUsersInGroup_devolveUsuariosDoGrupo() throws Exception {
        given(serviceGroup.listUsersInGroup(any(GroupRequest.class)))
                .willReturn(new ListUsersInGroupResponseDTO(
                        List.of(Map.of("name", "Fulano", "familyName", "de Tal")),
                        "siurb-frota"));

        mvc.perform(post("/api/admin/groups/users")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"groupName": "Frota", "groupCognito": "siurb-frota", \
                                "colorGroup": "#ff0000", "description": "Reserva de veiculos"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.group").value("siurb-frota"))
                .andExpect(jsonPath("$.username[0].name").value("Fulano"))
                .andExpect(jsonPath("$.username[0].familyName").value("de Tal"));
    }

    @Test
    void createGroup_valido_devolveGrupoCriado() throws Exception {
        given(serviceGroup.createGroup(any(GroupRequest.class)))
                .willReturn(new GroupResponse("Novo", "siurb-novo", "#0000ff", "Grupo novo"));

        mvc.perform(post("/api/admin/group")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"groupName": "Novo", "groupCognito": "siurb-novo", \
                                "colorGroup": "#0000ff", "description": "Grupo novo"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("Novo"))
                .andExpect(jsonPath("$.groupCognito").value("siurb-novo"))
                .andExpect(jsonPath("$.colorGroup").value("#0000ff"))
                .andExpect(jsonPath("$.description").value("Grupo novo"));

        verify(serviceGroup).createGroup(
                eq(new GroupRequest("Novo", "siurb-novo", "#0000ff", "Grupo novo")));
    }

    @Test
    void createGroup_semNome_barra400() throws Exception {
        mvc.perform(post("/api/admin/group")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"groupName": "", "groupCognito": "siurb-novo", \
                                "colorGroup": "#0000ff", "description": "Grupo sem nome"}"""))
                .andExpect(status().isBadRequest());

        verify(serviceGroup, never()).createGroup(any());
    }

    @Test
    void createGroup_semGroupCognito_barra400() throws Exception {
        mvc.perform(post("/api/admin/group")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"groupName": "Novo", "groupCognito": "", \
                                "colorGroup": "#0000ff", "description": "Grupo novo"}"""))
                .andExpect(status().isBadRequest());

        verify(serviceGroup, never()).createGroup(any());
    }

    @Test
    void createGroup_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(post("/api/admin/group")
                        .with(comGrupo("siurb-usuario-geral"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"groupName": "Novo", "groupCognito": "siurb-novo", \
                                "colorGroup": "#0000ff", "description": "Grupo novo"}"""))
                .andExpect(status().isForbidden());

        verify(serviceGroup, never()).createGroup(any());
    }

    @Test
    void putUserInGroups_adicionaUsuarioAoGrupo() throws Exception {
        given(serviceGroup.addUserToGroups(any(UserGroupsRequest.class)))
                .willReturn(new UserResponseMessage(
                        1L, "O usuario foi adicionado ao grupo!", "fulano@prefeitura.sp.gov.br"));

        mvc.perform(post("/api/admin/user/add-group")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username": "fulano@prefeitura.sp.gov.br", "group": "siurb-frota"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("O usuario foi adicionado ao grupo!"))
                .andExpect(jsonPath("$.given_name").value("fulano@prefeitura.sp.gov.br"));

        verify(serviceGroup).addUserToGroups(
                eq(new UserGroupsRequest("fulano@prefeitura.sp.gov.br", "siurb-frota")));
    }

    @Test
    void removeUserFromGroups_removeUsuarioDoGrupo() throws Exception {
        given(serviceGroup.removeUserFromGroups(any(UserGroupsRequest.class)))
                .willReturn(new UserResponseMessage(
                        1L, "O usuario foi removido do grupo", "fulano@prefeitura.sp.gov.br"));

        mvc.perform(post("/api/admin/user/remove-group")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username": "fulano@prefeitura.sp.gov.br", "group": "siurb-frota"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("O usuario foi removido do grupo"));

        verify(serviceGroup).removeUserFromGroups(
                eq(new UserGroupsRequest("fulano@prefeitura.sp.gov.br", "siurb-frota")));
    }

    @Test
    void deleteGroup_devolveMensagemDeSucesso() throws Exception {
        given(serviceGroup.deleteGroup(any(GroupRequest.class)))
                .willReturn(new GroupResponseMessage("siurb-frota", "Grupo deletado com sucesso!"));

        mvc.perform(delete("/api/admin/group")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"groupName": "Frota", "groupCognito": "siurb-frota", \
                                "colorGroup": "#ff0000", "description": "Reserva de veiculos"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("siurb-frota"))
                .andExpect(jsonPath("$.message").value("Grupo deletado com sucesso!"));
    }

    @Test
    void deleteGroup_nomeVazio_handlerDevolve400() throws Exception {
        given(serviceGroup.deleteGroup(any(GroupRequest.class)))
                .willThrow(new EmptyRequestException("eh necessario o nome do grupo"));

        mvc.perform(delete("/api/admin/group")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"groupName": "", "groupCognito": "", "colorGroup": "", "description": ""}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateGroup_devolveMensagemDeAtualizacao() throws Exception {
        given(serviceGroup.updateGroup(any(GroupRequest.class)))
                .willReturn(new GroupResponseMessage("siurb-frota", "O grupo foi atualizado"));

        mvc.perform(patch("/api/admin/group")
                        .with(comGrupo("siurb-administrador"))
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"groupName": "Frota", "groupCognito": "siurb-frota", \
                                "colorGroup": "#ff0000", "description": "Nova descricao"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupName").value("siurb-frota"))
                .andExpect(jsonPath("$.message").value("O grupo foi atualizado"));

        verify(serviceGroup).updateGroup(
                eq(new GroupRequest("Frota", "siurb-frota", "#ff0000", "Nova descricao")));
    }
}
