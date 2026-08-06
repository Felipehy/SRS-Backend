package com.siurbinfo.srs.service.room;

import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.entity.RoomEntity;
import com.siurbinfo.srs.mapper.room.RoomMapperImpl;
import com.siurbinfo.srs.repository.room.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RoomService.class, RoomMapperImpl.class})
class RoomServiceTest {

    @Autowired
    RoomService service;

    @Autowired
    RoomRepository repository;

    private RoomEntity persistRoom() {
        RoomEntity room = new RoomEntity();
        room.setFloor("ANDAR TESTE");
        return repository.save(room);
    }

    @Test
    void getAllRooms_returnsRooms() {
        persistRoom();

        List<RoomResponseDTO> all = service.getAllRooms();

        assertFalse(all.isEmpty());
    }

    @Test
    void getRoomFromId_returnsDto() {
        RoomEntity room = persistRoom();

        RoomResponseDTO dto = service.getRoomFromId(room.getId());

        assertNotNull(dto);
    }
}
