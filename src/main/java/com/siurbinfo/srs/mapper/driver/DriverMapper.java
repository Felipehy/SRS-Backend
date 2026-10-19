package com.siurbinfo.srs.mapper.driver;

import com.siurbinfo.srs.dto.driver.DriverRequestDTO;
import com.siurbinfo.srs.dto.driver.DriverResponseDTO;
import com.siurbinfo.srs.entity.driver.DriverEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    // licensePlate e modelCar ficam de fora: a API de motorista nao mexe nesses campos hoje,
    // eles so sao lidos (ex: e-mail de reserva de frota).
    @Mapping(target = "licensePlate", ignore = true)
    @Mapping(target = "modelCar", ignore = true)
    DriverEntity toEntity(DriverRequestDTO dto);
    List<DriverResponseDTO> toListResponseDTO(List<DriverEntity> entities);
    DriverResponseDTO toResponseDTO(DriverEntity entity);
    @Mapping(target = "licensePlate", ignore = true)
    @Mapping(target = "modelCar", ignore = true)
    void updateDriver(DriverRequestDTO dto, @MappingTarget DriverEntity entity);

}
