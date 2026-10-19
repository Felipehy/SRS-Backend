package com.siurbinfo.srs.service.department;

import com.siurbinfo.srs.dto.department.DepartmentRequestDTO;
import com.siurbinfo.srs.dto.department.DepartmentResponseDTO;
import com.siurbinfo.srs.entity.department.DepartmentEntity;
import com.siurbinfo.srs.entity.room.RoomEntity;
import com.siurbinfo.srs.exception.DepartmentIdNotFoundedException;
import com.siurbinfo.srs.exception.DepartmentInUseException;
import com.siurbinfo.srs.mapper.department.DepartmentMapper;
import com.siurbinfo.srs.repository.department.DepartmentRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DepartmentServiceTest {

    private DepartmentService service;
    private DepartmentRepository repository;
    private DepartmentMapper mapper;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        repository = mock(DepartmentRepository.class);
        mapper = mock(DepartmentMapper.class);
        userRepository = mock(UserRepository.class);
        service = new DepartmentService(repository, mapper, userRepository);
    }

    private static DepartmentEntity departamento(Long id, String name, Long idRoom, String floor) {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setId(id);
        entity.setName(name);
        if (idRoom != null) {
            RoomEntity room = new RoomEntity();
            room.setId(idRoom);
            room.setFloor(floor);
            entity.setRoom(room);
        }
        return entity;
    }

    @Test
    void getAllDepartment_mapeiaAndarDaSalaAssociada() {
        when(repository.getAllDepartment())
                .thenReturn(List.of(departamento(1L, "Tecnologia", 3L, "3 ANDAR")));

        List<DepartmentResponseDTO> result = service.getAllDepartment();

        assertEquals(1, result.size());
        assertEquals("Tecnologia", result.getFirst().name());
        assertEquals("3 ANDAR", result.getFirst().room());
        assertEquals(3L, result.getFirst().idRoom());
        assertEquals(1L, result.getFirst().id());
    }

    @Test
    void getAllDepartment_semSalaAssociada_naoQuebra() {
        when(repository.getAllDepartment())
                .thenReturn(List.of(departamento(1L, "Sem sala", null, null)));

        DepartmentResponseDTO dto = service.getAllDepartment().getFirst();

        assertNull(dto.room());
        assertNull(dto.idRoom());
        assertEquals("Sem sala", dto.name());
    }

    @Test
    void updateDepartment_idInexistente_lancaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(DepartmentIdNotFoundedException.class,
                () -> service.updateDepartment(new DepartmentRequestDTO("Tecnologia", 3L), 99L));

        verify(repository, never()).save(any());
    }

    @Test
    void updateDepartment_idExistente_aplicaOMapperESalva() {
        DepartmentEntity entity = departamento(7L, "Antigo", 3L, "3 ANDAR");
        when(repository.findById(7L)).thenReturn(Optional.of(entity));
        DepartmentRequestDTO dto = new DepartmentRequestDTO("Novo", 5L);

        service.updateDepartment(dto, 7L);

        verify(mapper).updateEntity(dto, entity);
        verify(repository).save(entity);
    }

    @Test
    void deleteDepartment_semUsuariosVinculados_apaga() {
        when(repository.existsById(7L)).thenReturn(true);
        when(userRepository.countByIdDepartment(7L)).thenReturn(0L);

        service.deleteDepartment(7L);

        verify(repository).deleteById(7L);
    }

    @Test
    void deleteDepartment_idInexistente_lancaNotFoundENaoApaga() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThrows(DepartmentIdNotFoundedException.class, () -> service.deleteDepartment(99L));

        verify(repository, never()).deleteById(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void deleteDepartment_comUsuariosVinculados_lancaInUseENaoApaga() {
        when(repository.existsById(7L)).thenReturn(true);
        when(userRepository.countByIdDepartment(7L)).thenReturn(3L);

        DepartmentInUseException ex =
                assertThrows(DepartmentInUseException.class, () -> service.deleteDepartment(7L));

        assertTrue(ex.getMessage().contains("3"));
        verify(repository, never()).deleteById(any());
    }
}
