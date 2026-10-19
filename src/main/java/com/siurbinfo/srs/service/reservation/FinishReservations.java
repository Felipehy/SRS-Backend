package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.repository.reservation.TaskFinishReservation;
import com.siurbinfo.srs.repository.reservation.projection.FinishReservationsProjection;
import com.siurbinfo.srs.service.smtp.SmtpService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FinishReservations {

    private static final Logger log = LoggerFactory.getLogger(FinishReservations.class);
    private final TaskFinishReservation repository;
    private final SmtpService smtpService;

    @Scheduled(fixedDelay = 15_000L)
    @Transactional
    public void finish(){
        try {
            List<FinishReservationsProjection> projections = repository.setFinishReservationsApproves();
            if (projections.isEmpty()) return;

            switchEmail(projections);

        } catch (Exception e) {
            throw new RuntimeException("Not has possible to found the reservations" + e.getMessage());
        }
    }

    private void switchEmail(List<FinishReservationsProjection> projections){
        List<ReserveType> listReservationsExists = new ArrayList<>();
        for (FinishReservationsProjection projection: projections){
            if (listReservationsExists.isEmpty() || !listReservationsExists.contains(projection.getReservationType()))
                listReservationsExists.add(projection.getReservationType());
        }

        for (ReserveType exists: listReservationsExists){
            if (exists.equals(ReserveType.AUDITORIUM)) sendEmailFinishAuditorium(projections);
            if (exists.equals(ReserveType.FLEET_VEHICLE)) sendEmailFinishFleetVehicle(projections);
            if (exists.equals(ReserveType.MEETING_ROOM)) sendEmailFinishMeetingRoom(projections);
        }

    }

    private void sendEmailFinishAuditorium(List<FinishReservationsProjection> reservations){

        List<Long> listIdsAuditoriumEntities = reservations.stream()
                        .filter(res -> res.getReservationType().equals(ReserveType.AUDITORIUM))
                        .map(FinishReservationsProjection::getId)
                        .toList();


        log.info("Send email Auditorium");
        for (AuditoriumEntity entity: repository.getAllReservationsAuditorium(listIdsAuditoriumEntities)){
            smtpService.sendEmail(entity);
        }
    }

    private void sendEmailFinishMeetingRoom(List<FinishReservationsProjection> reservations){
        List<Long> listIdsMeetingRoomEntities = reservations.stream()
                .filter(res -> res.getReservationType().equals(ReserveType.MEETING_ROOM))
                .map(FinishReservationsProjection::getId)
                .toList();

        log.info("Send email Meeting room");
        for (MeetingRoomEntity entity: repository.getAllReservationsMeetingRoom(listIdsMeetingRoomEntities)){
            smtpService.sendEmail(entity);
        }
    }

    private void sendEmailFinishFleetVehicle(List<FinishReservationsProjection> reservations){
        List<Long> listIdsFleetVehicleEntities = reservations.stream()
                .filter(res -> res.getReservationType().equals(ReserveType.FLEET_VEHICLE))
                .map(FinishReservationsProjection::getId)
                .toList();

        log.info("Send email Fleet vehicle");
        for (FleetVehicleEntity entity: repository.getAllReservationsFleetVehicle(listIdsFleetVehicleEntities)){
            smtpService.sendEmail(entity);
        }
    }
}
