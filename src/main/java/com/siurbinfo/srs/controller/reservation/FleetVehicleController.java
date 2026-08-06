package com.siurbinfo.srs.controller.reservation;

import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleRequestDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleResponseDTO;
import com.siurbinfo.srs.service.reservation.VehicleService;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller responsavel pelas reservas de veiculos da frota.
 * Expoe criacao, aprovacao/rejeicao, consulta (por id, todas ou por mes/ano),
 * atualizacao e remocao de reservas de veiculos.
 */
@RestController
@RequestMapping("/api/reservation/fleet-vehicle")
@RequiredArgsConstructor
public class FleetVehicleController {

    private final VehicleService vehicleService;

    /**
     * POST METHODS
     */

    // POST /api/reservation/fleet-vehicle - cria uma nova reserva de veiculo
    @PostMapping
    public ResponseEntity<Void> createReservationFleetVehicle(@RequestBody @Valid FleetVehicleRequestDTO dto) {
        vehicleService.createReservation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // POST /api/reservation/fleet-vehicle/{id}/approve - aprova a reserva
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> approveReservation(@PathVariable Long id){
        vehicleService.approve(id);
        return ResponseEntity.ok().build();
    }

    // POST /api/reservation/fleet-vehicle/{id}/reject - rejeita a reserva, informando motivo no corpo
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> rejectReservation(@PathVariable Long id,@RequestBody Map<String,String> body){
        vehicleService.reject(id,body);
        return ResponseEntity.ok().build();
    }

    /**
     * GET METHODS
     */

    // GET /api/reservation/fleet-vehicle - lista todas as reservas de veiculos
    @GetMapping
    public ResponseEntity<List<FleetVehicleResponseDTO>> getReservationFleetVehicle() {
        return ResponseEntity.ok().body(vehicleService.getAllReservation());
    }

    // GET /api/reservation/fleet-vehicle/{id} - busca uma reserva pelo id
    @GetMapping("/{id}")
    public ResponseEntity<?> getReservationFleetVehicleFromId(@PathVariable Long id) {
        return ResponseEntity.ok().body(vehicleService.getReservationFromId(id));
    }

    // GET /api/reservation/fleet-vehicle?year=&month= - lista reservas filtradas por mes e ano
    @GetMapping(params = {"year", "month"})
    public ResponseEntity<List<FleetVehicleResponseDTO>> getReservationFleetVehicleFromYearAndMonth(
            @RequestParam(name = "year") Integer year,
            @RequestParam(name = "month") Integer month) {
        return ResponseEntity.ok().body(vehicleService.getReservationFromYearAndMonth(year, month));
    }

    /**
     * PUT METHODS
     */

    // PUT /api/reservation/fleet-vehicle/{id} - atualiza os dados de uma reserva existente
    @PutMapping("/{id}")
    public ResponseEntity<Void> modifyReservationFleetVehicle(@PathVariable Long id, @RequestBody @Valid FleetVehicleRequestDTO dto) {
        vehicleService.updateReservation(id, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE METHODS
     */

    // DELETE /api/reservation/fleet-vehicle/{id} - remove uma reserva de veiculo
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> deleteReserveFleetVehicle(@PathVariable Long id) {
        vehicleService.deleteReserve(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
