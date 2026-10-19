package com.siurbinfo.srs.controller.reservation;

import com.siurbinfo.srs.dto.reservation.cancellation.CancellationDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleRequestDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleResponseDTO;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.service.log.LogSystemService;
import com.siurbinfo.srs.service.reservation.VehicleService;
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
@RequestMapping("/api/reservation/fleet-vehicle")
@RequiredArgsConstructor
public class FleetVehicleController {

    private final VehicleService vehicleService;
    private final LogSystemService logSystem;

    @PostMapping
    public ResponseEntity<Void> createReservationFleetVehicle(@RequestBody @Valid FleetVehicleRequestDTO dto, @AuthenticationPrincipal Jwt jwt) {
        FleetVehicleResponseDTO reservation = vehicleService.createReservation(dto,jwt);
        logSystem.log("Solicitação de uma nova reserva", "Veiculo da frota", ReserveType.FLEET_VEHICLE, reservation.id(), LogsType.CREATE, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancellReservation(@RequestBody CancellationDTO dto, @AuthenticationPrincipal Jwt jwt){
        vehicleService.cancelReservation(dto,jwt);
        logSystem.log("Cancelamento de reserva", "Veiculo da frota", ReserveType.FLEET_VEHICLE,dto.id(),LogsType.DELETE, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> approveReservation(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt){
        vehicleService.approve(id,jwt);
        logSystem.log("Aprovação da reserva","Veiculo da frota", ReserveType.FLEET_VEHICLE, id, LogsType.APPROVE, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> rejectReservation(@PathVariable Long id,@RequestBody Map<String,String> body, @AuthenticationPrincipal Jwt jwt){
        vehicleService.reject(id,body,jwt);
        logSystem.log("Rejeição de reserva","Veiculo da frota", ReserveType.FLEET_VEHICLE, id, LogsType.REJECT, jwt);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<FleetVehicleResponseDTO>> getReservationFleetVehicle() {
        return ResponseEntity.ok().body(vehicleService.getAllReservation());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReservationFleetVehicleFromId(@PathVariable Long id) {
        return ResponseEntity.ok().body(vehicleService.getReservationFromId(id));
    }

    @GetMapping(params = {"year", "month"})
    public ResponseEntity<List<FleetVehicleResponseDTO>> getReservationFleetVehicleFromYearAndMonth(
            @RequestParam(name = "year") Integer year,
            @RequestParam(name = "month") Integer month) {
        return ResponseEntity.ok().body(vehicleService.getReservationFromYearAndMonth(year, month));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> modifyReservationFleetVehicle(@PathVariable Long id, @RequestBody @Valid FleetVehicleRequestDTO dto, @AuthenticationPrincipal Jwt jwt) {
        vehicleService.updateReservation(id, dto, jwt);
        logSystem.log("Modificação de reservas","Veiculo da frota", ReserveType.FLEET_VEHICLE, id, LogsType.UPDATE, jwt);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-frotas')")
    public ResponseEntity<Void> deleteReserveFleetVehicle(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        vehicleService.deleteReserve(id);
        logSystem.log("Reserva excluida","Veiculo da frota", ReserveType.FLEET_VEHICLE, id, LogsType.DELETE, jwt);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
