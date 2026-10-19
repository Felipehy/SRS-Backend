package com.siurbinfo.srs.controller.reservation;

import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumResponseDTO;
import com.siurbinfo.srs.dto.reservation.cancellation.CancellationDTO;
import com.siurbinfo.srs.dto.reservation.reason.RejectCoffeeDTO;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.service.log.LogSystemService;
import com.siurbinfo.srs.service.reservation.AuditoriumService;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservation/auditorium")
@RequiredArgsConstructor
public class AuditoriumController {

    private final AuditoriumService auditoriumService;
    private final LogSystemService logSystem;

    @PostMapping
    public ResponseEntity<Void> createReservationAuditorium(@RequestBody @Valid AuditoriumRequestDTO dto, @AuthenticationPrincipal Jwt jwt) {
        AuditoriumResponseDTO reservation = auditoriumService.createReservation(dto,jwt);
        logSystem.log("Solicitação de uma nova reserva", "Auditorio", ReserveType.AUDITORIUM, reservation.id(), LogsType.CREATE, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancellReservation(@RequestBody CancellationDTO dto, @AuthenticationPrincipal Jwt jwt){
        auditoriumService.cancelReservation(dto,jwt);
        logSystem.log("Cancelamento de reserva", "Auditorio", ReserveType.AUDITORIUM,dto.id(),LogsType.DELETE, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> approveReservation(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt){
        auditoriumService.approve(id,jwt);
        logSystem.log("Aprovação da reserva","Administração", ReserveType.AUDITORIUM, id, LogsType.APPROVE, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> rejectReservation(@PathVariable Long id, @RequestBody Map<String,String> body, @AuthenticationPrincipal Jwt jwt){
        auditoriumService.reject(id,body,jwt);
        logSystem.log("Rejeição de reserva","Administração", ReserveType.AUDITORIUM, id, LogsType.REJECT, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/approve-coffee")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> approveCoffee(@RequestParam("id") Long id, @AuthenticationPrincipal Jwt jwt){
        auditoriumService.approveCoffee(id);
        logSystem.log("Aprovação do coffee break", "Administração", ReserveType.AUDITORIUM, id, LogsType.APPROVE, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reject-coffee")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> rejectCoffee(@RequestParam("id") Long id, @RequestBody RejectCoffeeDTO dto, @AuthenticationPrincipal Jwt jwt){
        auditoriumService.rejectCoffee(id,dto);
        logSystem.log("Rejeição do coffee break", "Administração", ReserveType.AUDITORIUM, id, LogsType.REJECT, jwt);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<AuditoriumResponseDTO>> getReservesAuditorium(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().body(auditoriumService.getAllReservation());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReservationAuditoriumFromId(@PathVariable Long id) {
        return ResponseEntity.ok().body(auditoriumService.getReserveFromId(id));
    }

    @GetMapping(params = {"year", "month"})
    public ResponseEntity<List<AuditoriumResponseDTO>> getReservationAuditoriumFromYearAndMonth(
            @RequestParam(value = "month", required = true) Integer month,
            @RequestParam(name = "year", required = true) Integer year) {
        return ResponseEntity.ok().body(auditoriumService.getReservationFromYearAndMonth(year, month));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> modifyReservationAuditorium(@PathVariable Long id, @RequestBody @Valid AuditoriumRequestDTO dto, @AuthenticationPrincipal Jwt jwt) {
        auditoriumService.updateReservation(id, dto);
        logSystem.log("Modificação de reservas","Auditorio", ReserveType.AUDITORIUM, id, LogsType.UPDATE, jwt);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> deleteReserveAuditorium(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        auditoriumService.deleteReservation(id);
        logSystem.log("Reserva excluida","Auditorio", ReserveType.AUDITORIUM, id, LogsType.DELETE, jwt);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
