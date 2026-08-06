package com.siurbinfo.srs.service.driver;

import com.siurbinfo.srs.dto.driver.DriverRequestDTO;
import com.siurbinfo.srs.dto.driver.DriverResponseDTO;
import com.siurbinfo.srs.entity.DriverEntity;
import com.siurbinfo.srs.exception.EmptyRequestException;
import com.siurbinfo.srs.mapper.driver.DriverMapper;
import com.siurbinfo.srs.repository.driver.DriverRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// Service responsavel pelo CRUD de motoristas (driver).
// Faz a conversao entre DTO e entidade via mapper e delega a persistencia ao repository.
@Service
public class DriverService {

    @Autowired
    DriverMapper mapper;

    @Autowired
    DriverRepository repository;

    // Cria um novo motorista, validando se ao menos um campo obrigatorio foi preenchido.
    public void create(DriverRequestDTO dto){
        DriverEntity data = mapper.toEntity(dto);
        // regra: nao permite salvar se imagem e nome vierem vazios ao mesmo tempo
        if(dto.img().isBlank() && dto.name().isBlank()){throw new EmptyRequestException("Todos os campos estão vazios");}
        repository.save(data);
    }

    // Retorna todos os motoristas cadastrados.
    public List<DriverResponseDTO> getAllDrivers(){
        return mapper.toListResponseDTO(repository.findAll());
    }

    // Busca um motorista pelo id.
    public DriverResponseDTO getDriverFromId(Long id){
        DriverEntity data = repository.getReferenceById(id);
        return mapper.toResponseDTO(data);
    }

    // Atualiza os dados de um motorista existente.
    public void update(DriverRequestDTO dto, Long id){
        DriverEntity data = repository.getReferenceById(id);
        // mesma regra de validacao usada na criacao
        if(dto.img().isBlank() && dto.name().isBlank()){throw new EmptyRequestException("Todos os campos estão vazios");}
        mapper.updateDriver(dto,data);
        repository.save(data);
    }

    // Remove um motorista pelo id, se ele existir.
    public void delete(Long id){
        if (repository.existsById(id)){
            repository.deleteById(id);
        }
    }

}
