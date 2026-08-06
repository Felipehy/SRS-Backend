package com.siurbinfo.srs.controller.reservation;

import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomRequestDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomResponseDTO;
import com.siurbinfo.srs.service.reservation.MeetingRoomService;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller responsavel pelas reservas de sala de reuniao.
 * Expoe criacao, aprovacao/rejeicao, consulta (por id, todas ou por mes/ano),
 * atualizacao e remocao de reservas de sala de reuniao.
 */
@RestController
@RequestMapping("/api/reservation/meeting-room")
@RequiredArgsConstructor
public class MeetingRoomController {

    private final MeetingRoomService meetingRoomService;

    /**
     * POST METHODS
     */

    // POST /api/reservation/meeting-room - cria uma nova reserva de sala de reuniao
    @PostMapping
    public ResponseEntity<Void> createReservationMeetingRoom(@RequestBody @Valid MeetingRoomRequestDTO dto) {
        meetingRoomService.createReservation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // POST /api/reservation/meeting-room/{id}/approve - aprova a reserva
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> approveReservation(@PathVariable Long id){
        meetingRoomService.approve(id);
        return ResponseEntity.ok().build();
    }

    // POST /api/reservation/meeting-room/{id}/reject - rejeita a reserva, informando motivo no corpo
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> rejectReservation(@PathVariable Long id,@RequestBody Map<String,String> body){
        meetingRoomService.reject(id,body);
        return ResponseEntity.ok().build();
    }

    /**
     * GET METHODS
     */

    // GET /api/reservation/meeting-room - lista todas as reservas de sala de reuniao
    @GetMapping
    public ResponseEntity<List<MeetingRoomResponseDTO>> getReservationMeetingRoom() {
        return ResponseEntity.ok().body(meetingRoomService.getAllReservation());
    }

    // GET /api/reservation/meeting-room/{id} - busca uma reserva pelo id
    @GetMapping("/{id}")
    public ResponseEntity<MeetingRoomResponseDTO> getReservationMeetingRoomFromId(@PathVariable Long id) {
        return ResponseEntity.ok().body(meetingRoomService.getReservationFromId(id));
    }

    // GET /api/reservation/meeting-room?year=&month= - lista reservas filtradas por mes e ano
    @GetMapping(params = {"year", "month"})
    public ResponseEntity<List<MeetingRoomResponseDTO>> getReservationMeetingRoomFromYearAndMonth(
            @RequestParam(name = "year") Integer year,
            @RequestParam(name = "month") Integer month) {
        return ResponseEntity.ok().body(meetingRoomService.getReservationFromYearAndMonth(year, month));
    }

    /**
     * PUT METHODS
     */

    // PUT /api/reservation/meeting-room/{id} - atualiza os dados de uma reserva existente
    @PutMapping("/{id}")
    public ResponseEntity<Void> modifyReservationMeetingRoom(@PathVariable Long id, @RequestBody @Valid MeetingRoomRequestDTO dto) {
        meetingRoomService.updateReservation(id, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * DELETE METHODS
     */

    // DELETE /api/reservation/meeting-room/{id} - remove uma reserva de sala de reuniao
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('siurb-administrador','siurb-divisao-administrativa')")
    public ResponseEntity<Void> deleteReserveMeetingRoom(@PathVariable Long id) {
        meetingRoomService.deleteReservation(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
