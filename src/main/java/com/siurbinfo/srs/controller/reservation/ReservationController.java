package com.siurbinfo.srs.controller.reservation;

import com.siurbinfo.srs.dto.reservation.ReservationsDTO;
import com.siurbinfo.srs.service.reservation.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService service;

    @GetMapping()
    public ResponseEntity<ReservationsDTO> getAllReservations(@RequestParam("startTime") LocalDate startTime,
                                                              @RequestParam("endTime") LocalDate endTime,
                                                              @AuthenticationPrincipal Jwt jwt){
        return ResponseEntity.ok(service.getAllReservations(startTime,endTime,jwt));
    }
}
