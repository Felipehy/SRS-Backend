package com.siurbinfo.srs.dto.reservation.meetingRoom;

import com.siurbinfo.srs.dto.room.RoomResponseDTO;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record MeetingRoomResponseDTO(

        Long id,
        LocalDate reservationDate,
        Instant dateAnalysis,
        RoomResponseDTO room,
        ReserveType reservationType,
        String reasonFailure,
        LocalTime startTime,
        String reasonOutsideHours,
        String userNameRequester,
        String requesterDepartment,
        String userNameApprover,
        LocalTime endTime,
        Integer numPeople,
        Boolean coffee,
        String reasonCoffee,
        Boolean isApproveCoffee,
        String reasonRejectCoffee,
        String observation,
        ReserveStatus status,
        Instant dateCreate,
        Long userId,
        String telephone,
        Instant dateCancellation,
        String reasonsCancellation

) {
}