package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomRequestDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomResponseDTO;
import com.siurbinfo.srs.entity.Reserve.MeetingRoomEntity;
import com.siurbinfo.srs.entity.RoomEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.RoomIdIsNotFoundedException;
import com.siurbinfo.srs.mapper.reserve.MeetingMapper;
import com.siurbinfo.srs.repository.reservation.ReservationMeetingRoomRepository;
import com.siurbinfo.srs.repository.room.RoomRepository;
import com.siurbinfo.srs.service.reservation.validators.MeetingRoomValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// Service de reservas de sala de reuniao: valida a reserva, resolve a sala (room)
// vinculada e controla o fluxo de status (analise, aprovacao, reprovacao).
@Service
@RequiredArgsConstructor
public class MeetingRoomService {

    private final MeetingMapper meetingMapper;
    private final ReservationMeetingRoomRepository meetingRoomRepositoryrepository;
    private final RoomRepository roomRepository;
    private final MeetingRoomValidator validator;

    // Cria uma reserva de sala, buscando a sala pelo id e definindo status inicial "enviada para analise".
    public void createReservation(MeetingRoomRequestDTO dto){
        validator.validateCreate(dto);
        MeetingRoomEntity reservation = meetingMapper.toEntity(dto);
        RoomEntity room = roomRepository.findById(dto.roomId())
                        .orElseThrow(() -> new RoomIdIsNotFoundedException("" + dto.roomId()));

        reservation.setReservationType(ReserveType.MEETING_ROOM);
        reservation.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        reservation.setRoom(room);
        meetingRoomRepositoryrepository.save(reservation);
    }

    // Aprova a reserva, apos validar se o status atual permite a transicao.
    @Transactional
    public void approve(Long id){
        validator.validatorApprove(id);
        meetingRoomRepositoryrepository.approveReservationFromId(id,ReserveStatus.APROVADA.name());
    }

    // Reprova a reserva com um motivo, apos validar se o status atual permite a transicao.
    @Transactional
    public void reject(Long id, Map<String,String> reasonFailure){
        validator.validatorReject(id);
        meetingRoomRepositoryrepository.rejectReservationFromId(id,ReserveStatus.REPROVADA.name(),reasonFailure.get("reasonFailure"));
    }

    // Lista todas as reservas de sala de reuniao.
    public List<MeetingRoomResponseDTO> getAllReservation() {
        List<MeetingRoomEntity> datas = meetingRoomRepositoryrepository.findAll()
                .stream()
                .toList();

        return meetingMapper.toListResponseDTO(datas);
    }

    // Busca uma reserva pelo id, lancando excecao se nao existir.
    public MeetingRoomResponseDTO getReservationFromId(Long id){
        MeetingRoomEntity data = meetingRoomRepositoryrepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));

        return meetingMapper.toResponseDTO(data);
    }

    // Lista as reservas de um determinado ano/mes.
    public List<MeetingRoomResponseDTO> getReservationFromYearAndMonth(Integer year, Integer month){
        List<MeetingRoomEntity> datas = meetingRoomRepositoryrepository.findByYearAndMonth(year, month);
        return meetingMapper.toListResponseDTO(datas);
    }

    // Atualiza uma reserva existente, revalidando as regras de negocio antes de salvar.
    public void updateReservation(Long id, MeetingRoomRequestDTO dto){
        MeetingRoomEntity entity = meetingRoomRepositoryrepository.findById(id)
                .orElseThrow(() -> new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id));

        validator.validateUpdate(dto,entity);
        meetingMapper.updateReservation(dto,entity);
        meetingRoomRepositoryrepository.save(entity);
    }

    // Remove uma reserva pelo id, lancando excecao se ela nao existir.
    public void deleteReservation(Long id) {
        if (meetingRoomRepositoryrepository.existsById(id)) {
            meetingRoomRepositoryrepository.deleteById(id);
        } else {
            throw new ReservationIdIsNotFoundedException("Reserva não encontrada: " + id);
        }
    }
}
