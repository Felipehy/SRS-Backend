package com.siurbinfo.srs.controller.log;

import com.siurbinfo.srs.dto.log.LogResponseDTO;
import com.siurbinfo.srs.service.log.LogSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('siurb-administrador')")
public class LogsController {

    private final LogSystemService logSystemService;

    @GetMapping()
    public ResponseEntity<List<LogResponseDTO>> getLogs(@RequestParam("offset") Long offSet){
        return ResponseEntity.ok().body(logSystemService.getLog(offSet));
    }

}
