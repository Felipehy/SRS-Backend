package com.siurbinfo.srs.controller.driver;

import com.siurbinfo.srs.dto.driver.DriverRequestDTO;
import com.siurbinfo.srs.dto.driver.DriverResponseDTO;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.service.driver.DriverService;
import com.siurbinfo.srs.service.log.LogSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService service;
    private final LogSystemService logSystem;

    @PostMapping
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> createDriver(@RequestBody DriverRequestDTO dto, @AuthenticationPrincipal Jwt jwt){
        DriverResponseDTO driver = service.create(dto);
        logSystem.log("Criação de um novo motorista", "Tela de frotas", null, driver.id(), LogsType.CREATE, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<DriverResponseDTO>> getAllDrivers(){
        return ResponseEntity.ok(service.getAllDrivers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverResponseDTO> getDriverFromId(@PathVariable Long id){
        return ResponseEntity.ok(service.getDriverFromId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> updateDriver(@PathVariable Long id, @RequestBody DriverRequestDTO dto, @AuthenticationPrincipal Jwt jwt){
        service.update(dto,id);
        logSystem.log("Modificação de dados do motorista", "Tela de frotas", null, id, LogsType.UPDATE, jwt);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt){
        service.delete(id);
        logSystem.log("Exclusão de motorista", "Tela de frotas", null, id, LogsType.DELETE, jwt);
        return ResponseEntity.ok().build();
    }
}
