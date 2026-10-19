package com.siurbinfo.srs.mapper.room;

import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.entity.room.RoomEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    RoomResponseDTO toResponseDTO(RoomEntity entity);

    @Mapping(source = "is_active", target = "isActive")
    List<RoomResponseDTO> toListResponseDTO(List<RoomEntity> entities);

}
