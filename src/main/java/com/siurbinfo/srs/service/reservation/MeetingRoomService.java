package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.cancellation.CancellationDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomRequestDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomResponseDTO;
import com.siurbinfo.srs.dto.reservation.reason.RejectCoffeeDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.entity.room.RoomEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.RoomIdIsNotFoundedException;
import com.siurbinfo.srs.exception.UserIdNotFoundedException;
import com.siurbinfo.srs.mapper.reserve.MeetingMapper;
import com.siurbinfo.srs.repository.reservation.ReservationMeetingRoomRepository;
import com.siurbinfo.srs.repository.room.RoomRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import com.siurbinfo.srs.service.reservation.validators.MeetingRoomValidator;
import com.siurbinfo.srs.service.smtp.SmtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingRoomService {

    private final MeetingMapper meetingMapper;
    private final ReservationMeetingRoomRepository meetingRoomRepositoryrepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final MeetingRoomValidator validator;
    private final CognitoUserService cognitoUserService;
    private final SmtpService smtpService;

    @Transactional
    public MeetingRoomResponseDTO createReservation(MeetingRoomRequestDTO dto,Jwt jwt){
        validator.validateCreate(dto,getGroupsFromUser(jwt));
        MeetingRoomEntity reservation = meetingMapper.toEntity(dto);
        RoomEntity room = roomRepository.findById(dto.roomId())
                        .orElseThrow(() -> new RoomIdIsNotFoundedException("" + dto.roomId()));
        reservation.setReasonOutsideHours(dto.reasonOutsideHours());
        reservation.setReservationType(ReserveType.MEETING_ROOM);
        reservation.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        reservation.setRoom(room);
        reservation.setUser(resolveUser(dto.userId()));
        meetingRoomRepositoryrepository.save(reservation);
        smtpService.sendEmail(reservation);
        return meetingMapper.toResponseDTO(reservation);
    }

    @Transactional
    public void approve(Long id, Jwt jwt){
        validator.validatorApprove(id);
        String name = cognitoUserService.getName(jwt.getTokenValue());
        Instant date = Instant.now();
        meetingRoomRepositoryrepository.approveReservationFromId(id,ReserveStatus.APROVADA.name(),name,date);
        MeetingRoomEntity reservation = meetingRoomRepositoryrepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reservation not founded, id: " + id));
        smtpService.sendEmail(reservation);
    }

    @Transactional
    public void reject(Long id, Map<String,String> reasonFailure, Jwt jwt){
        validator.validatorReject(id);
        String name = cognitoUserService.getName(jwt.getTokenValue());
        Instant date = Instant.now();
        meetingRoomRepositoryrepository.rejectReservationFromId(id,ReserveStatus.REPROVADA.name(),reasonFailure.get("reasonFailure"),name,date);
        MeetingRoomEntity reservation = meetingRoomRepositoryrepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reservation not founded, id: " + id));
        smtpService.sendEmail(reservation);
    }

    @Transactional
    public void approveCoffee(Long id){
        validator.validatorApproveCoffee(id);
        meetingRoomRepositoryrepository.approveCoffeeFromId(id);
    }

    @Transactional
    public void rejectCoffee(Long id, RejectCoffeeDTO dto){
        validator.validatorRejectCoffee(id);
        meetingRoomRepositoryrepository.rejectCoffeeFromId(id,dto.reasonRejectCoffee());
    }

    @Transactional
    public void cancelReservation(CancellationDTO dto, Jwt jwt){
        if (jwt == null) {
            throw new SecurityException("Is necessary a json web token(jwt)");
        }
        UserResponseDTO user = cognitoUserService.getUser(jwt);
        validator.validatorCancel(dto.id(), user.email(), getGroupsFromUser(jwt));
        meetingRoomRepositoryrepository.cancelReservation(dto.id(),user.id(), dto.reasonCancellation(), Instant.now());
        MeetingRoomEntity reservation = meetingRoomRepositoryrepository.findById(dto.id())
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reservation not founded, id: " + dto.id()));
        smtpService.sendEmail(reservation);
    }

    private List<String> getGroupsFromUser(Jwt jwt){
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        if (groups == null || groups.isEmpty()){
            throw new SecurityException("O usuário está sem grupos");
        }
        return groups;
    }

    public List<MeetingRoomResponseDTO> getAllReservation() {
        List<MeetingRoomEntity> datas = meetingRoomRepositoryrepository.findAll()
                .stream()
                .toList();

        return meetingMapper.toListResponseDTO(datas);
    }

    public MeetingRoomResponseDTO getReservationFromId(Long id){
        MeetingRoomEntity data = meetingRoomRepositoryrepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));

        return meetingMapper.toResponseDTO(data);
    }

    public List<MeetingRoomResponseDTO> getReservationFromYearAndMonth(Integer year, Integer month){
        List<MeetingRoomEntity> datas = meetingRoomRepositoryrepository.findByYearAndMonth(year, month);
        return meetingMapper.toListResponseDTO(datas);
    }

    public void updateReservation(Long id, MeetingRoomRequestDTO dto){
        MeetingRoomEntity entity = meetingRoomRepositoryrepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));

        validator.validateUpdate(dto,entity);
        meetingMapper.updateReservation(dto,entity);
        // sem userId no body o vinculo atual e mantido, em vez de ser apagado
        if (dto.userId() != null) {
            entity.setUser(resolveUser(dto.userId()));
        }
        meetingRoomRepositoryrepository.save(entity);
    }

    private UserEntity resolveUser(Long userId){
        if (userId == null) { throw new UserIdNotFoundedException("User not founded"); }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserIdNotFoundedException("Usuário não encontrado: " + userId));
    }

    public void deleteReservation(Long id) {
        if (meetingRoomRepositoryrepository.existsById(id)) {
            meetingRoomRepositoryrepository.deleteById(id);
        } else {
            throw new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id);
        }
    }
}
