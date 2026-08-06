package com.siurbinfo.srs.service.room;

import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.mapper.room.RoomMapper;
import com.siurbinfo.srs.repository.room.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

// Service simples de consulta de salas (rooms) disponiveis para reserva.
@Service
public class RoomService {

    @Autowired
    RoomMapper mapper;

    @Autowired
    RoomRepository repository;

    // Lista todas as salas cadastradas.
    public List<RoomResponseDTO> getAllRooms(){
        return mapper.toListResponseDTO(repository.findAll());
    }

    // Busca uma sala pelo id.
    public RoomResponseDTO getRoomFromId(Long id){
        return mapper.toResponseDTO(repository.getReferenceById(id));
    }

}
