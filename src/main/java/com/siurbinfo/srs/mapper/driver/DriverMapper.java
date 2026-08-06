package com.siurbinfo.srs.mapper.driver;

import com.siurbinfo.srs.dto.driver.DriverRequestDTO;
import com.siurbinfo.srs.dto.driver.DriverResponseDTO;
import com.siurbinfo.srs.entity.DriverEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Mapper entre DriverEntity e DriverRequestDTO/DriverResponseDTO.
 */
@Mapper(componentModel = "spring")
public interface DriverMapper {

    // DTO -> Entity
    DriverEntity toEntity(DriverRequestDTO dto);
    // List<Entity> -> List<ResponseDTO>
    List<DriverResponseDTO> toListResponseDTO(List<DriverEntity> entities);
    // Entity -> ResponseDTO
    DriverResponseDTO toResponseDTO(DriverEntity entity);
    // DTO -> atualiza Entity existente
    void updateDriver(DriverRequestDTO dto, @MappingTarget DriverEntity entity);

}
