package com.siurbinfo.srs.controller.room;

import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.service.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller responsavel pela consulta de salas cadastradas.
 * Apenas leitura: lista todas as salas ou busca uma sala especifica pelo id.
 */
@RestController
@RequestMapping("/api/room")
public class RoomController {

    @Autowired
    RoomService service;

    // GET /api/room - lista todas as salas cadastradas
    @GetMapping
    public ResponseEntity<List<RoomResponseDTO>> getAllRooms(){
        return ResponseEntity.ok(service.getAllRooms());
    }

    // GET /api/room/{id} - busca uma sala pelo id
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponseDTO> getRoomFromId(@PathVariable Long id){
        return ResponseEntity.ok(service.getRoomFromId(id));
    }
}
