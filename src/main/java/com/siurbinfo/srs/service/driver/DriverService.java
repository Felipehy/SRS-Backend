package com.siurbinfo.srs.service.driver;

import com.siurbinfo.srs.dto.driver.DriverRequestDTO;
import com.siurbinfo.srs.dto.driver.DriverResponseDTO;
import com.siurbinfo.srs.entity.driver.DriverEntity;
import com.siurbinfo.srs.exception.EmptyRequestException;
import com.siurbinfo.srs.mapper.driver.DriverMapper;
import com.siurbinfo.srs.repository.driver.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService {

    @Autowired
    DriverMapper mapper;

    @Autowired
    DriverRepository repository;

    public DriverResponseDTO create(DriverRequestDTO dto){
        DriverEntity data = mapper.toEntity(dto);
        // regra: nao permite salvar se imagem e nome vierem vazios ao mesmo tempo
        if(isBlank(dto.img()) && isBlank(dto.name())){throw new EmptyRequestException("Todos os campos estão vazios");}
        repository.save(data);
        return mapper.toResponseDTO(data);
    }

    public List<DriverResponseDTO> getAllDrivers(){
        return mapper.toListResponseDTO(repository.findAll());
    }

    public DriverResponseDTO getDriverFromId(Long id){
        DriverEntity data = repository.getReferenceById(id);
        return mapper.toResponseDTO(data);
    }

    public void update(DriverRequestDTO dto, Long id){
        DriverEntity data = repository.getReferenceById(id);
        if(isBlank(dto.img()) && isBlank(dto.name())){throw new EmptyRequestException("Todos os campos estão vazios");}
        mapper.updateDriver(dto,data);
        repository.save(data);
    }

    public void delete(Long id){
        if (repository.existsById(id)){
            repository.deleteById(id);
        }
    }

    private static boolean isBlank(String value){
        return value == null || value.isBlank();
    }

}
