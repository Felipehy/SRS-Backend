package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumResponseDTO;
import com.siurbinfo.srs.dto.reservation.cancellation.CancellationDTO;
import com.siurbinfo.srs.dto.reservation.reason.RejectCoffeeDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.UserIdNotFoundedException;
import com.siurbinfo.srs.mapper.reserve.AuditoriumMapper;
import com.siurbinfo.srs.repository.reservation.ReservationAuditoriumRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import com.siurbinfo.srs.service.glpi.GlpiService;
import com.siurbinfo.srs.service.reservation.validators.AuditoriumValidator;
import com.siurbinfo.srs.service.smtp.SmtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuditoriumService {

    private final ReservationAuditoriumRepository reserveAuditoriumRepository;
    private final AuditoriumMapper auditoriumMapper;
    private final AuditoriumValidator validator;
    private final CognitoUserService cognitoUserService;
    private final CognitoUserService userService;
    private final GlpiService glpiService;
    private final UserRepository userRepository;
    private final SmtpService smtpService;

    @Transactional
    public AuditoriumResponseDTO createReservation(AuditoriumRequestDTO dto,Jwt jwt){
        validator.validatorCreate(dto,getGroupsFromUser(jwt));
        AuditoriumEntity reservation = resolveReservation(dto);
        reserveAuditoriumRepository.save(reservation);
        smtpService.sendEmail(reservation);
        return auditoriumMapper.toResponseDTO(reservation);
    }

    private AuditoriumEntity resolveReservation(AuditoriumRequestDTO dto){
        AuditoriumEntity reservation = auditoriumMapper.toEntity(dto);
        reservation.setReasonOutsideHours(dto.reasonOutsideHours());
        reservation.setUser(resolveUser(dto.userId()));
        reservation.setReservationType(ReserveType.AUDITORIUM);
        reservation.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        return reservation;
    }

    @Transactional
    public void approve(Long id, Jwt jwt){
        validator.validatorApprove(id);
        reserveAuditoriumRepository.approveReservationFromId(id,
                ReserveStatus.APROVADA.name(),
                cognitoUserService.getName(jwt.getTokenValue()),
                Instant.now());
        glpiService.createTicket(id);

        AuditoriumEntity reservation = reserveAuditoriumRepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reservation not founded, id: " + id));
        smtpService.sendEmail(reservation);
    }

    @Transactional
    public void reject(Long id, Map<String,String> reasonFailure, Jwt jwt){
        validator.validatorReject(id);
        reserveAuditoriumRepository.rejectReservationFromId(id,
                ReserveStatus.REPROVADA.name(),
                reasonFailure.get("reasonFailure"),
                cognitoUserService.getName(jwt.getTokenValue()),
                Instant.now());

        AuditoriumEntity reservation = reserveAuditoriumRepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reservation not founded, id: " + id));
        smtpService.sendEmail(reservation);
    }

    @Transactional
    public void approveCoffee(Long id){
        validator.validatorApproveCoffee(id);
        reserveAuditoriumRepository.approveCoffeeFromId(id);
    }

    @Transactional
    public void rejectCoffee(Long id, RejectCoffeeDTO dto){
        validator.validatorRejectCoffee(id);
        reserveAuditoriumRepository.rejectCoffeeFromId(id,dto.reasonRejectCoffee());
    }

    @Transactional
    public void cancelReservation(CancellationDTO dto, Jwt jwt){
        if (jwt == null) {
            throw new SecurityException("Is necessary a json web token(jwt)");
        }
        UserResponseDTO user = userService.getUser(jwt);
        validator.validatorCancel(dto.id(), user.email(), getGroupsFromUser(jwt));
        reserveAuditoriumRepository.cancelReservation(dto.id(), user.id(), dto.reasonCancellation(), Instant.now());
        AuditoriumEntity reservation = reserveAuditoriumRepository.findById(dto.id())
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

    public List<AuditoriumResponseDTO> getAllReservation(){
        List<AuditoriumEntity> datas = reserveAuditoriumRepository.findAll()
                .stream()
                .toList();
        return auditoriumMapper.toListResponseDTO(datas);
    }

    public AuditoriumResponseDTO getReserveFromId(Long id){
        AuditoriumEntity data = reserveAuditoriumRepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));
        return auditoriumMapper.toResponseDTO(data);
    }

    public List<AuditoriumResponseDTO> getReservationFromYearAndMonth(Integer year,Integer month){
        List<AuditoriumEntity> datas = reserveAuditoriumRepository.findByYearAndMonth(year, month);
        return auditoriumMapper.toListResponseDTO(datas);
    }

    public void updateReservation(Long id, AuditoriumRequestDTO dto){
        AuditoriumEntity data = reserveAuditoriumRepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));
        validator.validatorUpdate(dto,data);
        auditoriumMapper.updateEntityFromDto(dto,data);
        // sem userId no body o vinculo atual e mantido, em vez de ser apagado
        if (dto.userId() != null) {
            data.setUser(resolveUser(dto.userId()));
        }
        reserveAuditoriumRepository.save(data);
    }

    private UserEntity resolveUser(Long userId){
        if (userId == null) { throw new UserIdNotFoundedException("User not founded"); }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserIdNotFoundedException("Usuário não encontrado: " + userId));
    }

    public void deleteReservation(Long id){
        if(reserveAuditoriumRepository.existsById(id)) {
            reserveAuditoriumRepository.deleteById(id);
        } else {
            throw new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id);
        }
    }

}
