package com.siurbinfo.srs.controller.smtp;

import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.service.log.LogSystemService;
import com.siurbinfo.srs.service.smtp.SmtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/smtp")
@RequiredArgsConstructor
public class SmtpController {

    private final LogSystemService logSystemService;
    private final SmtpService smtpService;

    @GetMapping
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<Boolean> getIsActiveSmtp(){
        return ResponseEntity.ok(smtpService.getIsActiveSmtp());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<Void> setIsActiveSmtp(@AuthenticationPrincipal Jwt jwt, @RequestParam("active") Boolean isActive){
        logSystemService.log("Modificação no SMTP", "Admin", null, null, LogsType.UPDATE, jwt);
        smtpService.setIsActiveSmtp(jwt, isActive);
        return ResponseEntity.ok().build();
    }

}
