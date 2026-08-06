package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumResponseDTO;
import com.siurbinfo.srs.entity.Reserve.AuditoriumEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.mapper.reserve.AuditoriumMapper;
import com.siurbinfo.srs.repository.reservation.ReservationAuditoriumRepository;
import com.siurbinfo.srs.service.reservation.validators.AuditoriumValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// Service de reservas de auditorio: orquestra validacao (via AuditoriumValidator),
// conversao DTO/entidade e persistencia, alem do fluxo de status (analise, aprovacao, reprovacao).
@Component
@RequiredArgsConstructor
public class AuditoriumService {

    private final ReservationAuditoriumRepository reserveAuditoriumRepository;
    private final AuditoriumMapper auditoriumMapper;
    private final AuditoriumValidator validator;

    // Cria uma nova reserva de auditorio, ja validada, com status inicial "enviada para analise".
    public void createReservation(AuditoriumRequestDTO dto){
        validator.validatorCreate(dto);
        AuditoriumEntity reserve = auditoriumMapper.toEntity(dto);
        reserve.setReservationType(ReserveType.AUDITORIUM);
        reserve.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        reserveAuditoriumRepository.save(reserve);
    }

    // Aprova a reserva, apos validar se o status atual permite a transicao.
    @Transactional
    public void approve(Long id){
        validator.validatorApprove(id);
        reserveAuditoriumRepository.approveReservationFromId(id,ReserveStatus.APROVADA.name());
    }

    // Reprova a reserva com um motivo, apos validar se o status atual permite a transicao.
    @Transactional
    public void reject(Long id, Map<String,String> reasonFailure){
        validator.validatorReject(id);
        reserveAuditoriumRepository.rejectReservationFromId(id,ReserveStatus.REPROVADA.name(),reasonFailure.get("reasonFailure"));
    }

    // Lista todas as reservas de auditorio.
    public List<AuditoriumResponseDTO> getAllReservation(){
        List<AuditoriumEntity> datas = reserveAuditoriumRepository.findAll()
                .stream()
                .toList();
        return auditoriumMapper.toListResponseDTO(datas);
    }

    // Busca uma reserva pelo id, lancando excecao se nao existir.
    public AuditoriumResponseDTO getReserveFromId(Long id){
        AuditoriumEntity data = reserveAuditoriumRepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));
        return auditoriumMapper.toResponseDTO(data);
    }

    // Lista as reservas de um determinado ano/mes.
    public List<AuditoriumResponseDTO> getReservationFromYearAndMonth(Integer year,Integer month){
        List<AuditoriumEntity> datas = reserveAuditoriumRepository.findByYearAndMonth(year, month);
        return auditoriumMapper.toListResponseDTO(datas);
    }

    // Atualiza uma reserva existente, revalidando as regras de negocio antes de salvar.
    public void updateReservation(Long id, AuditoriumRequestDTO dto){
        AuditoriumEntity data = reserveAuditoriumRepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));
        validator.validatorUpdate(dto,data);
        auditoriumMapper.updateEntityFromDto(dto,data);
        reserveAuditoriumRepository.save(data);
    }

    // Remove uma reserva pelo id, lancando excecao se ela nao existir.
    public void deleteReservation(Long id){
        if(reserveAuditoriumRepository.existsById(id)) {
            reserveAuditoriumRepository.deleteById(id);
        } else {
            throw new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id);
        }
    }

}
