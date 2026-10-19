package com.siurbinfo.srs.controller;

import com.siurbinfo.srs.controller.room.RoomController;
import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.service.log.LogSystemService;
import com.siurbinfo.srs.service.room.RoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class RoomControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RoomService service;

    @MockitoBean
    LogSystemService logSystemService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void getAllRooms_returns200() throws Exception {
        mvc.perform(get("/api/room"))
                .andExpect(status().isOk());
    }

    @Test
    void getRoomById_returns200() throws Exception {
        given(service.getRoomFromId(1L)).willReturn(new RoomResponseDTO(1L, "1 ANDAR", true));

        mvc.perform(get("/api/room/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.floor").value("1 ANDAR"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void setVisibleRoom_returns200() throws Exception {
        mvc.perform(patch("/api/room")
                        .param("id", "1")
                        .param("isActive", "false"))
                .andExpect(status().isOk());

        verify(service).setVisibleRoom(1L, false);
    }

    @Test
    void setVisibleRoom_semIsActive_returns400() throws Exception {
        mvc.perform(patch("/api/room").param("id", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setVisibleRoom_semId_returns400() throws Exception {
        mvc.perform(patch("/api/room").param("isActive", "true"))
                .andExpect(status().isBadRequest());
    }
}
