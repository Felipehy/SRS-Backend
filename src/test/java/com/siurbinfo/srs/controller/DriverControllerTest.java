package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.driver.DriverController;
import com.siurbinfo.srs.dto.driver.DriverResponseDTO;
import com.siurbinfo.srs.exception.EmptyRequestException;
import com.siurbinfo.srs.service.driver.DriverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DriverController.class)
@AutoConfigureMockMvc(addFilters = false)
class DriverControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DriverService service;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private static final String DRIVER_JSON = """
            {
              "name": "João Motorista",
              "img": "foto.png"
            }""";

    @Test
    void createDriver_returns201() throws Exception {
        mvc.perform(post("/api/driver")
                        .contentType(APPLICATION_JSON)
                        .content(DRIVER_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    void createDriver_emptyRequest_returns400() throws Exception {
        doThrow(new EmptyRequestException("campos vazios"))
                .when(service).create(any());

        mvc.perform(post("/api/driver")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"\",\"img\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllDrivers_returns200() throws Exception {
        mvc.perform(get("/api/driver"))
                .andExpect(status().isOk());
    }

    @Test
    void getDriverById_returns200() throws Exception {
        given(service.getDriverFromId(1L)).willReturn(new DriverResponseDTO(1L, "João Motorista", "foto.png"));

        mvc.perform(get("/api/driver/{id}", 1))
                .andExpect(status().isOk());
    }

    @Test
    void updateDriver_returns200() throws Exception {
        mvc.perform(put("/api/driver/{id}", 1)
                        .contentType(APPLICATION_JSON)
                        .content(DRIVER_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void deleteDriver_returns200() throws Exception {
        mvc.perform(delete("/api/driver/{id}", 1))
                .andExpect(status().isOk());
    }
}
