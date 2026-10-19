package com.siurbinfo.srs.controller.department;

import com.siurbinfo.srs.dto.department.DepartmentRequestDTO;
import com.siurbinfo.srs.dto.department.DepartmentResponseDTO;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import com.siurbinfo.srs.service.department.DepartmentService;
import com.siurbinfo.srs.service.log.LogSystemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/department")
@RequiredArgsConstructor
public class DepartmentController {

    private final LogSystemService log;
    private final DepartmentService service;
    private final CognitoUserService cognitoUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<List<DepartmentResponseDTO>> getAllDepartment(){
        return ResponseEntity.ok().body(service.getAllDepartment());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponseDTO> getDepartmentByUserId(@PathVariable("id") Long id, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok().body(cognitoUserService.getDepartmentFromUser(id,jwt));
    }

    @PatchMapping
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<Void> updateDepartment(@RequestParam("id") Long id, @Valid @RequestBody DepartmentRequestDTO dto, @AuthenticationPrincipal Jwt jwt){
        service.updateDepartment(dto, id);
        log.log("Alteração no departamento", "Admin", null, id, LogsType.UPDATE,jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<DepartmentResponseDTO> createDepartment(@Valid @RequestBody DepartmentRequestDTO dto, @AuthenticationPrincipal Jwt jwt){
        DepartmentResponseDTO response = service.createDepartment(dto);
        log.log("Criação de um novo departamento", "Admin", null, response.id(), LogsType.CREATE,jwt);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<Void> deleteDepartment(@RequestParam("id") Long id, @AuthenticationPrincipal Jwt jwt){
        service.deleteDepartment(id);
        log.log("Exclusão de um departamento", "Admin", null, id, LogsType.DELETE,jwt);
        return ResponseEntity.noContent().build();
    }

}
