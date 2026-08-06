package com.siurbinfo.srs.mapper.reserve;

import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleRequestDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleResponseDTO;
import com.siurbinfo.srs.entity.Reserve.FleetVehicleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Mapper entre FleetVehicleEntity e FleetVehicleRequestDTO/FleetVehicleResponseDTO.
 */
@Mapper(componentModel = "spring")
public interface VehicleMapper {

    // DTO -> Entity
    FleetVehicleEntity toEntity(FleetVehicleRequestDTO dto);
    // List<Entity> -> List<ResponseDTO>
    List<FleetVehicleResponseDTO> toResponseListDTO(List<FleetVehicleEntity> datas);
    // Entity -> ResponseDTO
    FleetVehicleResponseDTO toResponseDTO(FleetVehicleEntity entity);
    // DTO -> atualiza Entity existente
    void updateReserve(FleetVehicleRequestDTO dto, @MappingTarget FleetVehicleEntity data);

}
