package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.cancellation.CancellationDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleRequestDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleResponseDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.UserIdNotFoundedException;
import com.siurbinfo.srs.mapper.reserve.VehicleMapper;
import com.siurbinfo.srs.repository.reservation.ReservationFleetVehicleRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import com.siurbinfo.srs.service.reservation.validators.VehicleValidator;
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
public class VehicleService {

    private final VehicleMapper vehicleMapper;
    private final ReservationFleetVehicleRepository repository;
    private final VehicleValidator validator;
    private final UserRepository userRepository;
    private final CognitoUserService cognitoUserService;
    private final SmtpService smtpService;

    @Transactional
    public FleetVehicleResponseDTO createReservation(FleetVehicleRequestDTO dto,Jwt jwt){
        validator.validateCreate(dto, getGroupsFromUser(jwt));
        FleetVehicleEntity data = vehicleMapper.toEntity(dto);
        data.setUser(resolveUser(dto.userId()));
        data.setReservationType(ReserveType.FLEET_VEHICLE);
        data.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        repository.save(data);
        smtpService.sendEmail(data);
        return vehicleMapper.toResponseDTO(data);
    }

    @Transactional
    public void approve(Long id, Jwt jwt){
        validator.validatorApprove(id);
        String name = cognitoUserService.getName(jwt.getTokenValue());
        Instant date = Instant.now();
        repository.approveReservationFromId(id,ReserveStatus.APROVADA.name(),name,date);

        FleetVehicleEntity reservation = repository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reservation not founded, id: " + id));
        smtpService.sendEmail(reservation);
    }

    @Transactional
    public void reject(Long id, Map<String,String> reasonFailure, Jwt jwt){
        validator.validatorReject(id);
        String name = cognitoUserService.getName(jwt.getTokenValue());
        Instant date = Instant.now();
        repository.rejectReservationFromId(id,ReserveStatus.REPROVADA.name(),reasonFailure.get("reasonFailure"),name,date);
        FleetVehicleEntity reservation = repository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reservation not founded, id: " + id));
        smtpService.sendEmail(reservation);
    }

    @Transactional
    public void cancelReservation(CancellationDTO dto, Jwt jwt){
        if (jwt == null) {
            throw new SecurityException("Is necessary a json web token(jwt)");
        }
        UserResponseDTO user = cognitoUserService.getUser(jwt);
        validator.validatorCancel(dto.id(), user.email(), getGroupsFromUser(jwt));
        repository.cancelReservation(dto.id(),user.id(), dto.reasonCancellation(), Instant.now());
        FleetVehicleEntity reservation = repository.findById(dto.id())
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

    public List<FleetVehicleResponseDTO> getAllReservation(){
        List<FleetVehicleEntity> datas = repository.findAll()
                .stream()
                .toList();
        return vehicleMapper.toResponseListDTO(datas);
    }

    public FleetVehicleResponseDTO getReservationFromId(Long id){
        FleetVehicleEntity data = repository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));
        return vehicleMapper.toResponseDTO(data);
    }

    public List<FleetVehicleResponseDTO> getReservationFromYearAndMonth(Integer year, Integer month){
        List<FleetVehicleEntity> datas = repository.findByYearAndMonth(year, month);
        return vehicleMapper.toResponseListDTO(datas);
    }

    public void updateReservation(Long id, FleetVehicleRequestDTO dto, Jwt jwt){
        FleetVehicleEntity data = repository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));
        validator.validateUpdate(dto, data, getGroupsFromUser(jwt));
        vehicleMapper.updateReserve(dto, data);
        // sem userId no body o vinculo atual e mantido, em vez de ser apagado
        if (dto.userId() != null) {
            data.setUser(resolveUser(dto.userId()));
        }
        repository.save(data);
    }

    private UserEntity resolveUser(Long userId){
        if (userId == null) { throw new UserIdNotFoundedException("User not founded"); }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserIdNotFoundedException("Usuário não encontrado: " + userId));
    }

    public void deleteReserve(Long id){
        if (repository.existsById(id)){
            repository.deleteById(id);
        } else {
            throw new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id);
        }
    }

}
