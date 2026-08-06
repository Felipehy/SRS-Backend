package com.siurbinfo.srs.controller.reservation;

import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumResponseDTO;
import com.siurbinfo.srs.service.reservation.AuditoriumService;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller responsavel pelas reservas de auditorio.
 * Expoe criacao, aprovacao/rejeicao, consulta (por id, todas ou por mes/ano),
 * atualizacao e remocao de reservas de auditorio.
 */
@RestController
@RequestMapping("/api/reservation/auditorium")
@RequiredArgsConstructor
public class AuditoriumController {

    private final AuditoriumService auditoriumService;

    /**
     * POST METHODS
     */

    // POST /api/reservation/auditorium - cria uma nova reserva de auditorio
    @PostMapping
    public ResponseEntity<Void> createReservationAuditorium(@RequestBody @Valid AuditoriumRequestDTO dto) {
        auditoriumService.createReservation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // POST /api/reservation/auditorium/{id}/approve - aprova a reserva
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> approveReservation(@PathVariable Long id){
        auditoriumService.approve(id);
        return ResponseEntity.ok().build();
    }

    // POST /api/reservation/auditorium/{id}/reject - rejeita a reserva, informando motivo no corpo
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> rejectReservation(@PathVariable Long id, @RequestBody Map<String,String> body){
        auditoriumService.reject(id,body);
        return ResponseEntity.ok().build();
    }

    /**
     * GET METHODS
     */

    // GET /api/reservation/auditorium - lista todas as reservas de auditorio
    @GetMapping
    public ResponseEntity<List<AuditoriumResponseDTO>> getReservesAuditorium() {
        return ResponseEntity.ok().body(auditoriumService.getAllReservation());
    }

    // GET /api/reservation/auditorium/{id} - busca uma reserva pelo id
    @GetMapping("/{id}")
    public ResponseEntity<?> getReservationAuditoriumFromId(@PathVariable Long id) {
        return ResponseEntity.ok().body(auditoriumService.getReserveFromId(id));
    }

    // GET /api/reservation/auditorium?year=&month= - lista reservas filtradas por mes e ano
    @GetMapping(params = {"year", "month"})
    public ResponseEntity<List<AuditoriumResponseDTO>> getReservationAuditoriumFromYearAndMonth(
            @RequestParam(value = "month", required = true) Integer month,
            @RequestParam(name = "year", required = true) Integer year) {
        return ResponseEntity.ok().body(auditoriumService.getReservationFromYearAndMonth(year, month));
    }

    /**
     * PUT METHODS
     */

    // PUT /api/reservation/auditorium/{id} - atualiza os dados de uma reserva existente
    @PutMapping("/{id}")
    public ResponseEntity<Void> modifyReservationAuditorium(@PathVariable Long id, @RequestBody @Valid AuditoriumRequestDTO dto) {
        auditoriumService.updateReservation(id, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE METHODS
     */

    // DELETE /api/reservation/auditorium/{id} - remove uma reserva de auditorio
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> deleteReserveAuditorium(@PathVariable Long id) {
        auditoriumService.deleteReservation(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
