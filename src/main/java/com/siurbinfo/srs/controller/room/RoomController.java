package com.siurbinfo.srs.controller.room;

import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.service.log.LogSystemService;
import com.siurbinfo.srs.service.room.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room")
public class RoomController {

    @Autowired
    RoomService service;

    @Autowired
    LogSystemService log;

    @GetMapping
    public ResponseEntity<List<RoomResponseDTO>> getAllRooms(){
        return ResponseEntity.ok(service.getAllRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponseDTO> getRoomFromId(@PathVariable Long id){
        return ResponseEntity.ok(service.getRoomFromId(id));
    }

    @PatchMapping()
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<Void> setVisibleRoom(@RequestParam("id") Long id, @RequestParam("isActive") Boolean isActive, @AuthenticationPrincipal Jwt jwt){
        service.setVisibleRoom(id, isActive);
        log.log("Modificado as salas disponives", "admin", null, null, LogsType.UPDATE, jwt);
        return ResponseEntity.ok().build();
    }

}
