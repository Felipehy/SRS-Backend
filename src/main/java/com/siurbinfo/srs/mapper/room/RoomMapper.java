package com.siurbinfo.srs.mapper.room;

import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.entity.RoomEntity;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Mapper de RoomEntity para RoomResponseDTO (somente leitura, sem DTO de request).
 */
@Mapper(componentModel = "spring")
public interface RoomMapper {

    // Entity -> ResponseDTO
    RoomResponseDTO toResponseDTO(RoomEntity entity);
    // List<Entity> -> List<ResponseDTO>
    List<RoomResponseDTO> toListResponseDTO(List<RoomEntity> entities);

}
