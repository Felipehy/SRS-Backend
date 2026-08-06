package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleRequestDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleResponseDTO;
import com.siurbinfo.srs.entity.Reserve.FleetVehicleEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.mapper.reserve.VehicleMapper;
import com.siurbinfo.srs.repository.reservation.ReservationFleetVehicleRepository;
import com.siurbinfo.srs.service.reservation.validators.VehicleValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// Service de reservas de veiculo da frota: valida a reserva e controla o fluxo
// de status (analise, aprovacao, reprovacao).
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleMapper vehicleMapper;
    private final ReservationFleetVehicleRepository repository;
    private final VehicleValidator validator;

    // Cria uma reserva de veiculo com status inicial "enviada para analise".
    public void createReservation(FleetVehicleRequestDTO dto){
        validator.validateCreate(dto);
        FleetVehicleEntity data = vehicleMapper.toEntity(dto);
        data.setReservationType(ReserveType.FLEET_VEHICLE);
        data.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        repository.save(data);
    }

    // Aprova a reserva, apos validar se o status atual permite a transicao.
    @Transactional
    public void approve(Long id){
        validator.validatorApprove(id);
        repository.approveReservationFromId(id,ReserveStatus.APROVADA.name());
    }

    // Reprova a reserva com um motivo, apos validar se o status atual permite a transicao.
    @Transactional
    public void reject(Long id, Map<String,String> reasonFailure){
        validator.validatorReject(id);
        repository.rejectReservationFromId(id,ReserveStatus.REPROVADA.name(),reasonFailure.get("reasonFailure"));
    }

    // Lista todas as reservas de veiculo.
    public List<FleetVehicleResponseDTO> getAllReservation(){
        List<FleetVehicleEntity> datas = repository.findAll()
                .stream()
                .toList();
        return vehicleMapper.toResponseListDTO(datas);
    }

    // Busca uma reserva pelo id, lancando excecao se nao existir.
    public FleetVehicleResponseDTO getReservationFromId(Long id){
        FleetVehicleEntity data = repository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));
        return vehicleMapper.toResponseDTO(data);
    }

    // Lista as reservas de um determinado ano/mes.
    public List<FleetVehicleResponseDTO> getReservationFromYearAndMonth(Integer year, Integer month){
        List<FleetVehicleEntity> datas = repository.findByYearAndMonth(year, month);
        return vehicleMapper.toResponseListDTO(datas);
    }

    // Atualiza uma reserva existente, revalidando as regras de negocio antes de salvar.
    public void updateReservation(Long id, FleetVehicleRequestDTO dto){
        FleetVehicleEntity data = repository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));
        validator.validateUpdate(dto, data);
        vehicleMapper.updateReserve(dto, data);
        repository.save(data);
    }

    // Remove uma reserva pelo id, lancando excecao se ela nao existir.
    public void deleteReserve(Long id){
        if (repository.existsById(id)){
            repository.deleteById(id);
        } else {
            throw new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id);
        }
    }

}
