package com.siurbinfo.srs.service.driver;

import com.siurbinfo.srs.dto.driver.DriverRequestDTO;
import com.siurbinfo.srs.dto.driver.DriverResponseDTO;
import com.siurbinfo.srs.entity.DriverEntity;
import com.siurbinfo.srs.exception.EmptyRequestException;
import com.siurbinfo.srs.mapper.driver.DriverMapperImpl;
import com.siurbinfo.srs.repository.driver.DriverRepository;
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
@Import({DriverService.class, DriverMapperImpl.class})
class DriverServiceTest {

    @Autowired
    DriverService service;

    @Autowired
    DriverRepository repository;

    private DriverRequestDTO validDto() {
        return new DriverRequestDTO("João Motorista", "foto.png");
    }

    // ---------- create ----------

    @Test
    void create_persistsDriver() {
        service.create(validDto());

        List<DriverEntity> all = repository.findAll();
        assertEquals(1, all.size());
        assertEquals("João Motorista", all.getFirst().getName());
    }

    @Test
    void create_allFieldsBlank_throws() {
        assertThrows(EmptyRequestException.class,
                () -> service.create(new DriverRequestDTO("", "")));
        assertEquals(0, repository.count());
    }

    // ---------- getAllDrivers / getDriverFromId ----------

    @Test
    void getAllDrivers_returnsAll() {
        service.create(validDto());

        List<DriverResponseDTO> all = service.getAllDrivers();

        assertEquals(1, all.size());
    }

    @Test
    void getDriverFromId_returnsDto() {
        service.create(validDto());
        Long id = repository.findAll().getFirst().getId();

        DriverResponseDTO dto = service.getDriverFromId(id);

        assertNotNull(dto);
    }

    // ---------- update ----------

    @Test
    void update_changesName() {
        service.create(validDto());
        Long id = repository.findAll().getFirst().getId();

        service.update(new DriverRequestDTO("Maria Motorista", "nova.png"), id);

        DriverEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals("Maria Motorista", reloaded.getName());
    }

    @Test
    void update_allFieldsBlank_throws() {
        service.create(validDto());
        Long id = repository.findAll().getFirst().getId();

        assertThrows(EmptyRequestException.class,
                () -> service.update(new DriverRequestDTO("", ""), id));
    }

    // ---------- delete ----------

    @Test
    void delete_removesExisting() {
        service.create(validDto());
        Long id = repository.findAll().getFirst().getId();

        service.delete(id);

        assertFalse(repository.existsById(id));
    }

    @Test
    void delete_notFound_doesNothing() {
        assertDoesNotThrow(() -> service.delete(999_999L));
    }
}
