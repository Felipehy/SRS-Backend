package com.siurbinfo.srs.service.room;

import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.mapper.room.RoomMapper;
import com.siurbinfo.srs.repository.room.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomService {

    @Autowired
    RoomMapper mapper;

    @Autowired
    RoomRepository repository;

    public List<RoomResponseDTO> getAllRooms(){
        return mapper.toListResponseDTO(repository.getRooms());
    }

    public RoomResponseDTO getRoomFromId(Long id){
        return mapper.toResponseDTO(repository.getReferenceById(id));
    }

    @Transactional
    public void setVisibleRoom(Long id, Boolean isActive){repository.setVisible(id, isActive);}

}
