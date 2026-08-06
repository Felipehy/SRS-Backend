package com.siurbinfo.srs.controller.efs;

import com.siurbinfo.srs.dto.efs.EfsResponseDTO;
import com.siurbinfo.srs.service.efs.EfsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/anexos")
@RequiredArgsConstructor
public class EfsController {

    private final EfsService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EfsResponseDTO> uploud(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.uploud(file, jwt.getTokenValue()));
    }

    @GetMapping("/img")
    public ResponseEntity<Resource> getImg(@AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(service.getImg(jwt.getTokenValue()));
    }

}
