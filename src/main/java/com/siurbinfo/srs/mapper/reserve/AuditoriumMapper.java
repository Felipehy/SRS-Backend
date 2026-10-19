package com.siurbinfo.srs.mapper.reserve;

import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumResponseDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.CoffeeBreakDTO;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

// O campo user fica de fora: o DTO manda so o userId e quem resolve o UserEntity e o AuditoriumService.
@Mapper(componentModel = "spring")
public interface AuditoriumMapper {

    @Mapping(target = "user", ignore = true)
    AuditoriumEntity toEntity(AuditoriumRequestDTO dto);

    @Mapping(source = "userId", target = "user.id")
    AuditoriumEntity toEntity(AuditoriumResponseDTO dto);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "data", target = "coffeeBreak")
    AuditoriumResponseDTO toResponseDTO(AuditoriumEntity data);

    // no response os campos do coffee break vao agrupados; na entidade continuam soltos
    CoffeeBreakDTO toCoffeeBreakDTO(AuditoriumEntity data);

    List<AuditoriumResponseDTO> toListResponseDTO(List<AuditoriumEntity> datas);

    @Mapping(target = "user", ignore = true)
    void updateEntityFromDto(AuditoriumRequestDTO dto, @MappingTarget AuditoriumEntity data);

}
