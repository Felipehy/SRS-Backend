package com.siurbinfo.srs.dto.reservation.meetingRoom;

import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Resposta com os dados de uma reserva de sala de reuniao
public record MeetingRoomResponseDTO(

        Long id,

        LocalDate reservationDate,

        LocalDateTime dateAnalysis,

        RoomResponseDTO room,

        ReserveType reservationType,

        String reasonFailure,

        LocalTime startTime,

        String userNameRequester,

        String userNameApprover,

        LocalTime endTime,

        Integer numPeople,

        Boolean coffee,

        String observation,

        ReserveStatus status,

        LocalDateTime dateCreate

) {
}