package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.smtp.SmtpController;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.exception.EntityNotFoundException;
import com.siurbinfo.srs.service.log.LogSystemService;
import com.siurbinfo.srs.service.smtp.SmtpService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SmtpController.class)
@Import(SmtpControllerTest.SecurityTestConfig.class)
@ActiveProfiles("test")
class SmtpControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SmtpService smtpService;

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
    void getIsActiveSmtp_comAdministrador_devolveOEstadoAtual() throws Exception {
        given(smtpService.getIsActiveSmtp()).willReturn(false);

        mvc.perform(get("/api/smtp").with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void getIsActiveSmtp_comDivisaoAdministrativa_barra403() throws Exception {
        mvc.perform(get("/api/smtp").with(comGrupo("siurb-divisao-administrativa")))
                .andExpect(status().isForbidden());

        verify(smtpService, never()).getIsActiveSmtp();
    }

    @Test
    void getIsActiveSmtp_comUsuarioGeral_barra403() throws Exception {
        mvc.perform(get("/api/smtp").with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());

        verify(smtpService, never()).getIsActiveSmtp();
    }

    @Test
    void setIsActiveSmtp_comAdministrador_desligaERegistraLog() throws Exception {
        mvc.perform(post("/api/smtp")
                        .param("active", "false")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk());

        verify(smtpService).setIsActiveSmtp(any(Jwt.class), eq(false));
        verify(logSystemService).log(eq("Modificação no SMTP"), eq("Admin"),
                eq(null), eq(null), eq(LogsType.UPDATE), any(Jwt.class));
    }

    @Test
    void setIsActiveSmtp_comAdministrador_religa() throws Exception {
        mvc.perform(post("/api/smtp")
                        .param("active", "true")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isOk());

        verify(smtpService).setIsActiveSmtp(any(Jwt.class), eq(true));
    }

    @Test
    void setIsActiveSmtp_semActive_barra400() throws Exception {
        mvc.perform(post("/api/smtp").with(comGrupo("siurb-administrador")))
                .andExpect(status().isBadRequest());

        verify(smtpService, never()).setIsActiveSmtp(any(), anyBoolean());
    }

    @Test
    void setIsActiveSmtp_activeInvalido_barra400() throws Exception {
        mvc.perform(post("/api/smtp")
                        .param("active", "talvez")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isBadRequest());

        verify(smtpService, never()).setIsActiveSmtp(any(), anyBoolean());
    }

    @Test
    void setIsActiveSmtp_comUsuarioGeral_barra403SemLogNemAlteracao() throws Exception {
        mvc.perform(post("/api/smtp")
                        .param("active", "false")
                        .with(comGrupo("siurb-usuario-geral")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(smtpService, logSystemService);
    }

    @Test
    void setIsActiveSmtp_semLinhaDeConfig_barra400() throws Exception {
        willThrow(new EntityNotFoundException("Nenhuma configuração foi encontrada!"))
                .given(smtpService).setIsActiveSmtp(any(Jwt.class), eq(false));

        mvc.perform(post("/api/smtp")
                        .param("active", "false")
                        .with(comGrupo("siurb-administrador")))
                .andExpect(status().isBadRequest());
    }
}
