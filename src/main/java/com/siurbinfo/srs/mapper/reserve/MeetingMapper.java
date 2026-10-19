package com.siurbinfo.srs.mapper.reserve;

import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomRequestDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomResponseDTO;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

// Os campos room e user ficam de fora: quem busca e seta a sala e o usuario e o MeetingRoomService.
@Mapper(componentModel = "spring")
public interface MeetingMapper {

    @Mapping(target = "room", ignore = true)
    @Mapping(target = "user", ignore = true)
    MeetingRoomEntity toEntity(MeetingRoomRequestDTO dto);

    @Mapping(source = "coffee", target = "isCoffee")
    @Mapping(source = "userId", target = "user.id")
    MeetingRoomEntity toEntity(MeetingRoomResponseDTO dto);

    @Mapping(source = "isCoffee", target = "coffee")
    @Mapping(source = "user.id", target = "userId")
    List<MeetingRoomResponseDTO> toListResponseDTO(List<MeetingRoomEntity> entities);

    @Mapping(source = "isCoffee", target = "coffee")
    @Mapping(source = "user.id", target = "userId")
    MeetingRoomResponseDTO toResponseDTO (MeetingRoomEntity entity);

    @Mapping(target = "room", ignore = true)
    @Mapping(target = "user", ignore = true)
    void updateReservation(MeetingRoomRequestDTO dto, @MappingTarget MeetingRoomEntity entity);
}
