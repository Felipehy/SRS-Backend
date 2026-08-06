package com.siurbinfo.srs.mapper.reserve;

import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumResponseDTO;
import com.siurbinfo.srs.entity.Reserve.AuditoriumEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Mapper entre AuditoriumEntity e AuditoriumRequestDTO/AuditoriumResponseDTO.
 */
@Mapper(componentModel = "spring")
public interface AuditoriumMapper {

    // DTO -> Entity
    AuditoriumEntity toEntity(AuditoriumRequestDTO dto);

    // Entity -> ResponseDTO
    AuditoriumResponseDTO toResponseDTO(AuditoriumEntity data);

    // List<Entity> -> List<ResponseDTO>
    List<AuditoriumResponseDTO> toListResponseDTO(List<AuditoriumEntity> datas);

    // DTO -> atualiza Entity existente
    void updateEntityFromDto(AuditoriumRequestDTO dto, @MappingTarget AuditoriumEntity data);

}
