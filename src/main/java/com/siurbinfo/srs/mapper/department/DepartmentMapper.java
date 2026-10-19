package com.siurbinfo.srs.mapper.department;

import com.siurbinfo.srs.dto.department.DepartmentRequestDTO;
import com.siurbinfo.srs.dto.department.DepartmentResponseDTO;
import com.siurbinfo.srs.entity.department.DepartmentEntity;
import com.siurbinfo.srs.entity.room.RoomEntity;
import com.siurbinfo.srs.repository.room.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

// Classe abstrata (e nao interface) porque o mapeamento idRoom -> RoomEntity
// precisa consultar o RoomRepository.
@Mapper(componentModel = "spring")
public abstract class DepartmentMapper {

    @Autowired
    protected RoomRepository roomRepository;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", source = "idRoom")
    public abstract DepartmentEntity toEntity(DepartmentRequestDTO dto);

    @Mapping(target = "room", source = "room.floor")
    @Mapping(target = "idRoom", source = "room.id")
    public abstract DepartmentResponseDTO toResponseDTO(DepartmentEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "room", source = "idRoom")
    public abstract DepartmentEntity updateEntity(DepartmentRequestDTO dto, @MappingTarget DepartmentEntity entity);

    // Usado automaticamente pelo MapStruct para converter Long -> RoomEntity
    protected RoomEntity toRoom(Long idRoom) {
        if (idRoom == null) {
            return null;
        }
        return roomRepository.findById(idRoom)
                .orElseThrow(() -> new EntityNotFoundException("Não foi encontrada a sala: " + idRoom));
    }

}
