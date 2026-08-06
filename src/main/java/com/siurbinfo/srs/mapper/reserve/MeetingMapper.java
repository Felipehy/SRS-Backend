package com.siurbinfo.srs.mapper.reserve;

import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomRequestDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomResponseDTO;
import com.siurbinfo.srs.entity.Reserve.MeetingRoomEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Mapper entre MeetingRoomEntity e MeetingRoomRequestDTO/MeetingRoomResponseDTO.
 */
@Mapper(componentModel = "spring")
public interface MeetingMapper {

    // DTO -> Entity (campo room e ignorado, associado a parte)
    @Mapping(target = "room", ignore = true)
    MeetingRoomEntity toEntity(MeetingRoomRequestDTO dto);

    // List<Entity> -> List<ResponseDTO>
    List<MeetingRoomResponseDTO> toListResponseDTO(List<MeetingRoomEntity> entities);

    // Entity -> ResponseDTO
    MeetingRoomResponseDTO toResponseDTO (MeetingRoomEntity entity);

    // DTO -> atualiza Entity existente (campo room e ignorado)
    @Mapping(target = "room", ignore = true)
    void updateReservation(MeetingRoomRequestDTO dto, @MappingTarget MeetingRoomEntity entity);
}
