package com.siurbinfo.srs.controller.reservation;

import com.siurbinfo.srs.dto.reservation.cancellation.CancellationDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomRequestDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomResponseDTO;
import com.siurbinfo.srs.dto.reservation.reason.RejectCoffeeDTO;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.service.log.LogSystemService;
import com.siurbinfo.srs.service.reservation.MeetingRoomService;
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
@RequestMapping("/api/reservation/meeting-room")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;
    private final LogSystemService logSystem;

    @PostMapping
    public ResponseEntity<Void> createReservationMeetingRoom(@RequestBody @Valid MeetingRoomRequestDTO dto, @AuthenticationPrincipal Jwt jwt) {
        MeetingRoomResponseDTO reservation = meetingRoomService.createReservation(dto,jwt);
        logSystem.log("Solicitação de uma nova reserva", "Sala de reuniao", ReserveType.MEETING_ROOM, reservation.id(), LogsType.CREATE, jwt);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancellReservation(@RequestBody CancellationDTO dto, @AuthenticationPrincipal Jwt jwt){
        meetingRoomService.cancelReservation(dto,jwt);
        logSystem.log("Cancelamento de reserva", "Sala de reuniao", ReserveType.MEETING_ROOM,dto.id(),LogsType.DELETE, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> approveReservation(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt){
        meetingRoomService.approve(id,jwt);
        logSystem.log("Aprovação da reserva","Sala de reuniao", ReserveType.MEETING_ROOM, id, LogsType.APPROVE, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> rejectReservation(@PathVariable Long id,@RequestBody Map<String,String> body, @AuthenticationPrincipal Jwt jwt){
        meetingRoomService.reject(id,body,jwt);
        logSystem.log("Rejeição de reserva","Sala de reuniao", ReserveType.MEETING_ROOM, id, LogsType.REJECT, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/approve-coffee")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> approveCoffee(@RequestParam("id") Long id, @AuthenticationPrincipal Jwt jwt){
        meetingRoomService.approveCoffee(id);
        logSystem.log("Aprovação do coffee break", "Administração", ReserveType.MEETING_ROOM, id, LogsType.APPROVE, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reject-coffee")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> rejectCoffee(@RequestParam("id") Long id, @RequestBody RejectCoffeeDTO dto, @AuthenticationPrincipal Jwt jwt){
        meetingRoomService.rejectCoffee(id,dto);
        logSystem.log("Rejeição do coffee break", "Administração", ReserveType.MEETING_ROOM, id, LogsType.REJECT, jwt);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<MeetingRoomResponseDTO>> getReservationMeetingRoom() {
        return ResponseEntity.ok().body(meetingRoomService.getAllReservation());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeetingRoomResponseDTO> getReservationMeetingRoomFromId(@PathVariable Long id) {
        return ResponseEntity.ok().body(meetingRoomService.getReservationFromId(id));
    }

    @GetMapping(params = {"year", "month"})
    public ResponseEntity<List<MeetingRoomResponseDTO>> getReservationMeetingRoomFromYearAndMonth(
            @RequestParam(name = "year") Integer year,
            @RequestParam(name = "month") Integer month) {
        return ResponseEntity.ok().body(meetingRoomService.getReservationFromYearAndMonth(year, month));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> modifyReservationMeetingRoom(@PathVariable Long id, @RequestBody @Valid MeetingRoomRequestDTO dto, @AuthenticationPrincipal Jwt jwt) {
        meetingRoomService.updateReservation(id, dto);
        logSystem.log("Modificação de reservas","Sala de reuniao", ReserveType.MEETING_ROOM, id, LogsType.UPDATE, jwt);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> deleteReserveMeetingRoom(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        meetingRoomService.deleteReservation(id);
        logSystem.log("Reserva excluida","Sala de reuniao", ReserveType.MEETING_ROOM, id, LogsType.DELETE, jwt);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
