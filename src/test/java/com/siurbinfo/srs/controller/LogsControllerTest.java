package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.log.LogsController;
import com.siurbinfo.srs.dto.log.LogResponseDTO;
import com.siurbinfo.srs.service.log.LogSystemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LogsController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LogsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LogSystemService logSystemService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void getLogs_returns200() throws Exception {
        given(logSystemService.getLog(1L)).willReturn(List.of(
                new LogResponseDTO(1L, "AUDITORIUM", "criou reserva", "auditorio", "Fulano", "de Tal",
                        "fulano@prefeitura.sp.gov.br", "200.1.2.3", "{siurb-administrador}", "CREATE",
                        Instant.parse("2026-08-20T10:30:00Z"), 42L)));

        mvc.perform(get("/api/logs").param("offset", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].typeLog").value("CREATE"))
                .andExpect(jsonPath("$[0].email").value("fulano@prefeitura.sp.gov.br"))
                .andExpect(jsonPath("$[0].ip").value("200.1.2.3"));
    }

    @Test
    void getLogs_repassaOffsetParaOService() throws Exception {
        given(logSystemService.getLog(3L)).willReturn(List.of());

        mvc.perform(get("/api/logs").param("offset", "3"))
                .andExpect(status().isOk());

        verify(logSystemService).getLog(3L);
    }

    @Test
    void getLogs_semOffset_returns400() throws Exception {
        mvc.perform(get("/api/logs"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getLogs_offsetInvalido_returns400() throws Exception {
        mvc.perform(get("/api/logs").param("offset", "abc"))
                .andExpect(status().isBadRequest());
    }
}
