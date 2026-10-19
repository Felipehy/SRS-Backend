package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.efs.EfsController;
import com.siurbinfo.srs.dto.efs.EfsResponseDTO;
import com.siurbinfo.srs.exception.FileNotExistInEfsException;
import com.siurbinfo.srs.exception.InvalidFileException;
import com.siurbinfo.srs.service.efs.EfsService;
import com.siurbinfo.srs.service.log.LogSystemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EfsController.class)
@Import(EfsControllerTest.SecurityTestConfig.class)
@ActiveProfiles("test")
class EfsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    EfsService service;

    @MockitoBean
    LogSystemService logSystemService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @TestConfiguration
    @EnableWebSecurity
    static class SecurityTestConfig {
        @Bean
        SecurityFilterChain chain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    void deleteImg_returns200_andDelegatesToService() throws Exception {
        mvc.perform(delete("/api/anexos/delete").with(jwt()))
                .andExpect(status().isOk());

        verify(service).deleteImg("token");
    }

    @Test
    void uploud_returns201_comOsDadosDoArquivo() throws Exception {
        given(service.uploud(any(), any()))
                .willReturn(new EfsResponseDTO("foto.jpg", 1024L, MediaType.IMAGE_JPEG_VALUE));

        MockMultipartFile file = new MockMultipartFile(
                "file", "foto.jpg", MediaType.IMAGE_JPEG_VALUE, "conteudo".getBytes());

        mvc.perform(multipart("/api/anexos").file(file).with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("foto.jpg"))
                .andExpect(jsonPath("$.fileLength").value(1024))
                .andExpect(jsonPath("$.contentType").value(MediaType.IMAGE_JPEG_VALUE));

        verify(service).uploud(any(), org.mockito.ArgumentMatchers.eq("token"));
    }

    @Test
    void uploud_arquivoInvalido_returns400() throws Exception {
        doThrow(new InvalidFileException("arquivo invalido"))
                .when(service).uploud(any(), any());

        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "conteudo".getBytes());

        mvc.perform(multipart("/api/anexos").file(file).with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploud_semArquivo_returns400() throws Exception {
        mvc.perform(multipart("/api/anexos").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getImg_returns200ComoImagemECache() throws Exception {
        given(service.getImg("token")).willReturn(resourceFake());

        mvc.perform(get("/api/anexos/img").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=3600"));
    }

    @Test
    void getImg_arquivoInexistente_returns404() throws Exception {
        given(service.getImg("token")).willThrow(new FileNotExistInEfsException("sem imagem"));

        mvc.perform(get("/api/anexos/img").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getImgByParam_returns200() throws Exception {
        given(service.getImgByParam("fulano@prefeitura.sp.gov.br")).willReturn(resourceFake());

        mvc.perform(get("/api/anexos/imgs").param("email", "fulano@prefeitura.sp.gov.br").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE));

        verify(service).getImgByParam("fulano@prefeitura.sp.gov.br");
    }

    @Test
    void getImgByParam_semEmail_returns400() throws Exception {
        mvc.perform(get("/api/anexos/imgs").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    private static Resource resourceFake() {
        return new ByteArrayResource("imagem".getBytes()) {
            @Override
            public String getFilename() {
                return "foto.jpg";
            }
        };
    }
}
