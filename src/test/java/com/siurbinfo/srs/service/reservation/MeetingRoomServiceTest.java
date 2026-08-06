package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomRequestDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomResponseDTO;
import com.siurbinfo.srs.entity.Reserve.MeetingRoomEntity;
import com.siurbinfo.srs.entity.RoomEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.InvalidPeopleCountException;
import com.siurbinfo.srs.exception.InvalidTimeBetweenReservations;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.ReservationWasApprove;
import com.siurbinfo.srs.exception.ReservationWasCancell;
import com.siurbinfo.srs.exception.RoomIdIsNotFoundedException;
import com.siurbinfo.srs.exception.StartTimeIsNotBeforeEndTimeException;
import com.siurbinfo.srs.mapper.reserve.MeetingMapperImpl;
import com.siurbinfo.srs.repository.reservation.ReservationMeetingRoomRepository;
import com.siurbinfo.srs.repository.room.RoomRepository;
import com.siurbinfo.srs.service.reservation.validators.MeetingRoomValidator;
import com.siurbinfo.srs.service.reservation.validators.ReservationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do MeetingRoomService.
 *
 * O setup de estado (aprovar / reprovar / buscar / atualizar / deletar) é feito
 * inserindo a entidade DIRETAMENTE no repositório, e não via createReservation(),
 * para não acoplar esses testes ao fluxo de criação — que hoje está quebrado por
 * um bug de PRODUÇÃO (ver relatório):
 *
 *   - BUG A: ReservationValidator.isStartiTimeStartAfterATime chama reservation.get()
 *            num Optional vazio -> NoSuchElementException na PRIMEIRA reserva do dia
 *            (quando não existe reserva anterior para comparar o intervalo).
 *
 * Os testes de intervalo mínimo (gap) inserem uma reserva anterior antes de chamar
 * o service, de modo que o Optional NÃO fica vazio — por isso eles já passam com o
 * código atual e exercitam a nova validação isStartiTimeStartAfterATime (15 min).
 *
 * Os testes marcados com "[BLOQUEADO POR BUG A]" só passam depois da correção.
 * NÃO alterei o código de produção.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        MeetingRoomService.class,
        MeetingRoomValidator.class,
        ReservationValidator.class,
        MeetingMapperImpl.class
})
class MeetingRoomServiceTest {

    @Autowired
    MeetingRoomService service;

    @Autowired
    ReservationMeetingRoomRepository repository;

    @Autowired
    RoomRepository roomRepository;

    private RoomEntity room;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 1);

    @BeforeEach
    void setUp() {
        room = new RoomEntity();
        room.setFloor("ANDAR TESTE");
        room = roomRepository.save(room);
    }

    /** DTO válido apontando para a sala criada no setUp. */
    private MeetingRoomRequestDTO validDto() {
        return new MeetingRoomRequestDTO(
                DATE,
                room.getId(),
                LocalTime.of(9, 0),    // startTime
                "Fulano de Tal",       // userNameRequester
                LocalTime.of(10, 0),   // endTime
                5,                     // numPeople
                Boolean.TRUE,          // isCoffee
                "Reunião de teste"     // observation
        );
    }

    /** Insere uma reserva diretamente no banco, sem passar pelo service/validator. */
    private MeetingRoomEntity persistReservation(LocalTime start, LocalTime end) {
        MeetingRoomEntity e = new MeetingRoomEntity();
        e.setReservationDate(DATE);
        e.setRoom(room);
        e.setStartTime(start);
        e.setEndTime(end);
        e.setUserNameRequester("Fulano de Tal");
        e.setNumPeople(5);
        e.setIsCoffee(Boolean.TRUE);
        e.setReservationType(ReserveType.MEETING_ROOM);
        e.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        return repository.saveAndFlush(e);
    }

    /** Insere uma reserva diretamente no banco com o status informado. */
    private MeetingRoomEntity persistWithStatus(ReserveStatus status) {
        MeetingRoomEntity e = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0));
        e.setStatus(status);
        return repository.saveAndFlush(e);
    }

    // ---------- createReservation ----------

    @Test
    void createReservation_startTimeNotBeforeEndTime_throws() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(10, 0), "Fulano",
                LocalTime.of(9, 0), 5, Boolean.FALSE, null);

        assertThrows(StartTimeIsNotBeforeEndTimeException.class,
                () -> service.createReservation(dto));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_invalidPeopleCount_throws() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(9, 0), "Fulano",
                LocalTime.of(10, 0), 0, Boolean.FALSE, null);

        assertThrows(InvalidPeopleCountException.class,
                () -> service.createReservation(dto));
        assertEquals(0, repository.count());
    }

    @Test
        // [BLOQUEADO POR BUG A] primeira reserva do dia -> NoSuchElementException
    void createReservation_persistsWithDefaultStatusAndType() {
        service.createReservation(validDto());

        List<MeetingRoomEntity> all = repository.findAll();
        assertEquals(1, all.size());

        MeetingRoomEntity saved = all.getFirst();
        assertNotNull(saved.getId());
        assertEquals(ReserveType.MEETING_ROOM, saved.getReservationType());
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, saved.getStatus());
        assertEquals(room.getId(), saved.getRoom().getId());
        assertEquals(5, saved.getNumPeople());
        assertTrue(saved.getIsCoffee());
    }

    @Test
        // [BLOQUEADO POR BUG A] validateCreate roda antes da busca da sala e a
        // validação de intervalo estoura no Optional vazio antes do RoomIdIsNotFounded
    void createReservation_roomNotFound_throws() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, 999_999L, LocalTime.of(9, 0), "Fulano",
                LocalTime.of(10, 0), 5, Boolean.FALSE, null);

        assertThrows(RoomIdIsNotFoundedException.class,
                () -> service.createReservation(dto));
        assertEquals(0, repository.count());
    }

    @Test
        // [BLOQUEADO POR BUG A] a primeira reserva (inserida via service) já estoura
    void createReservation_sameRoomAndDate_throwsConflict() {
        persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertThrows(ReservationConflictException.class,
                () -> service.createReservation(validDto()));
        assertEquals(1, repository.count());
    }

    // ---------- nova validação: intervalo mínimo entre reservas (15 min) ----------

    @Test
    void createReservation_lessThanMinGapFromPrevious_throwsInvalidTime() {
        // Reserva anterior terminando às 10:00
        persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0));

        // Nova reserva começando às 10:10 -> apenas 10 min de intervalo (< 15)
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(10, 10), "Fulano",
                LocalTime.of(11, 0), 5, Boolean.FALSE, null);

        assertThrows(InvalidTimeBetweenReservations.class,
                () -> service.createReservation(dto));
        assertEquals(1, repository.count());
    }

    @Test
    void createReservation_sufficientGapFromPrevious_succeeds() {
        // Reserva anterior terminando às 10:00
        persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0));

        // Nova reserva começando às 10:20 -> 20 min de intervalo (> 15) e sem sobreposição
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(10, 20), "Fulano",
                LocalTime.of(11, 0), 5, Boolean.TRUE, null);

        service.createReservation(dto);

        assertEquals(2, repository.count());
    }

    // ---------- getReservationFromId / getAllReservation ----------

    @Test
    void getReservationFromId_returnsDto() {
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        MeetingRoomResponseDTO dto = service.getReservationFromId(id);

        assertEquals(id, dto.id());
        assertNotNull(dto.room());
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, dto.status());
    }

    @Test
    void getReservationFromId_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.getReservationFromId(999_999L));
    }

    @Test
    void getAllReservation_returnsAll() {
        persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0));

        List<MeetingRoomResponseDTO> all = service.getAllReservation();

        assertEquals(1, all.size());
    }

    // ---------- approve / reject ----------

    @Test
        // [BLOQUEADO POR BUG D] validatorApprove -> findStatusById devolve a entidade
    void approve_setsStatusApproved() {
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        service.approve(id);

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.APROVADA, reloaded.getStatus());
    }

    @Test
        // [BLOQUEADO POR BUG D] guarda nova isPossibleApprove
    void approve_alreadyApproved_throws() {
        Long id = persistWithStatus(ReserveStatus.APROVADA).getId();

        assertThrows(ReservationWasApprove.class, () -> service.approve(id));
    }

    @Test
        // [BLOQUEADO POR BUG D] validatorReject -> findStatusById devolve a entidade
    void reject_setsStatusRejectedWithReason() {
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        service.reject(id, Map.of("reasonFailure", "Conflito de agenda"));

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.REPROVADA, reloaded.getStatus());
        assertEquals("Conflito de agenda", reloaded.getReasonFailure());
    }

    @Test
        // [BLOQUEADO POR BUG D] guarda nova isPossibleReject
    void reject_alreadyCancelled_throws() {
        Long id = persistWithStatus(ReserveStatus.CANCELADA).getId();

        assertThrows(ReservationWasCancell.class,
                () -> service.reject(id, Map.of("reasonFailure", "Conflito de agenda")));
    }

    // ---------- updateReservation ----------

    @Test
    void updateReservation_updatesExisting() {
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        // A validação de update (isSameReservationDate) exige uma data DIFERENTE da atual.
        MeetingRoomRequestDTO update = new MeetingRoomRequestDTO(
                DATE.plusDays(1), room.getId(), LocalTime.of(9, 0), "Fulano",
                LocalTime.of(11, 0), 8, Boolean.FALSE, "Atualizada");

        service.updateReservation(id, update);

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(8, reloaded.getNumPeople());
        assertEquals(LocalTime.of(11, 0), reloaded.getEndTime());
        assertFalse(reloaded.getIsCoffee());
    }

    @Test
    void updateReservation_sameDate_throwsConflict() {
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        // Mantendo a MESMA data, a nova validação isSameReservationDate dispara conflito.
        assertThrows(ReservationConflictException.class,
                () -> service.updateReservation(id, validDto()));
    }

    @Test
    void updateReservation_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.updateReservation(999_999L, validDto()));
    }

    // ---------- deleteReservation ----------

    @Test
    void deleteReservation_removesExisting() {
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        service.deleteReservation(id);

        assertFalse(repository.existsById(id));
    }

    @Test
    void deleteReservation_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.deleteReservation(999_999L));
    }
}
