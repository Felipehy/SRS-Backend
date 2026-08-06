package com.siurbinfo.srs.controller.driver;

import com.siurbinfo.srs.dto.driver.DriverRequestDTO;
import com.siurbinfo.srs.dto.driver.DriverResponseDTO;
import com.siurbinfo.srs.service.driver.DriverService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller responsavel pelo cadastro de motoristas (drivers).
 * Permite criar, listar, buscar por id, atualizar e remover motoristas.
 * Operacoes de escrita (criar/atualizar/deletar) exigem role de administrador ou divisao de frotas.
 */
@RestController
@RequestMapping("/api/driver")
public class DriverController {

    @Autowired
    DriverService service;


    // POST /api/driver - cria um novo motorista
    @PostMapping
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> createDriver(@RequestBody DriverRequestDTO dto){
        service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // GET /api/driver - lista todos os motoristas cadastrados
    @GetMapping
    public ResponseEntity<List<DriverResponseDTO>> getAllDrivers(){
        return ResponseEntity.ok(service.getAllDrivers());
    }

    // GET /api/driver/{id} - busca um motorista pelo id
    @GetMapping("/{id}")
    public ResponseEntity<DriverResponseDTO> getDriverFromId(@PathVariable Long id){
        return ResponseEntity.ok(service.getDriverFromId(id));
    }

    // PUT /api/driver/{id} - atualiza os dados de um motorista existente
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> updateDriver(@PathVariable Long id, @RequestBody DriverRequestDTO dto){
        service.update(dto,id);
        return ResponseEntity.ok().build();
    }

    // DELETE /api/driver/{id} - remove um motorista
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> deleteDriver(@PathVariable Long id){
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
