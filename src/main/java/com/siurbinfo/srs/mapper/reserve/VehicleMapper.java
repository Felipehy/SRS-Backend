package com.siurbinfo.srs.mapper.reserve;

import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleRequestDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleResponseDTO;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

// O campo user fica de fora: o DTO manda so o userId e quem resolve o UserEntity e o VehicleService.
@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "user", ignore = true)
    FleetVehicleEntity toEntity(FleetVehicleRequestDTO dto);

    @Mapping(source = "userId", target = "user.id")
    FleetVehicleEntity toEntity(FleetVehicleResponseDTO dto);

    List<FleetVehicleResponseDTO> toResponseListDTO(List<FleetVehicleEntity> datas);

    @Mapping(source = "user.id", target = "userId")
    FleetVehicleResponseDTO toResponseDTO(FleetVehicleEntity entity);

    @Mapping(target = "user", ignore = true)
    void updateReserve(FleetVehicleRequestDTO dto, @MappingTarget FleetVehicleEntity data);

}
