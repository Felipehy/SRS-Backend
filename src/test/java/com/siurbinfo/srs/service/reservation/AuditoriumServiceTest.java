package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumResponseDTO;
import com.siurbinfo.srs.entity.Reserve.AuditoriumEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.InvalidPeopleCountException;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.ReservationWasApprove;
import com.siurbinfo.srs.exception.ReservationWasCancell;
import com.siurbinfo.srs.exception.StartTimeIsNotBeforeEndTimeException;
import com.siurbinfo.srs.mapper.reserve.AuditoriumMapperImpl;
import com.siurbinfo.srs.repository.reservation.ReservationAuditoriumRepository;
import com.siurbinfo.srs.service.reservation.validators.AuditoriumValidator;
import com.siurbinfo.srs.service.reservation.validators.ReservationValidator;
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
 * Testes do AuditoriumService.
 *
 * O setup de estado (aprovar / reprovar / buscar / atualizar / deletar) é feito
 * inserindo a entidade DIRETAMENTE no repositório, e não via createReservation(),
 * para que esses testes não fiquem acoplados ao fluxo de criação — que hoje está
 * quebrado por bugs de PRODUÇÃO (ver relatório):
 *
 *   - BUG A: ReservationValidator.isStartiTimeStartAfterATime chama reservation.get()
 *            num Optional vazio -> NoSuchElementException na PRIMEIRA reserva do dia.
 *   - BUG C: ReservationAuditoriumRepository.findByReservationDateAndStartTime é uma
 *            query derivada que devolve a entidade, mas está declarada como
 *            Optional<LocalTime> -> ClassCastException quando existe linha.
 *
 * Os testes marcados com "[BLOQUEADO POR BUG ...]" só passam depois que o bug de
 * produção correspondente for corrigido. NÃO alterei o código de produção.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuditoriumService.class, AuditoriumMapperImpl.class, AuditoriumValidator.class, ReservationValidator.class})
class AuditoriumServiceTest {

    @Autowired
    AuditoriumService service;

    @Autowired
    ReservationAuditoriumRepository repository;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 1);

    private AuditoriumRequestDTO validDto() {
        return new AuditoriumRequestDTO(
                DATE,
                LocalTime.of(8, 30),   // timeOpenAuditorium
                "Palestra de teste",   // reason
                LocalTime.of(9, 0),    // startTime
                LocalTime.of(11, 0),   // endTime
                "Fulano de Tal",       // userNameRequester
                50,                    // numPeople
                Boolean.FALSE,         // externalPublic
                Boolean.TRUE,          // necessaryScreen
                Boolean.TRUE,          // necessarySoundSystem
                Boolean.FALSE          // necessaryCoffee
        );
    }

    /** Insere uma reserva diretamente no banco, sem passar pelo service/validator. */
    private AuditoriumEntity persistReservation(LocalDate date, LocalTime start, LocalTime end) {
        AuditoriumEntity e = new AuditoriumEntity();
        e.setReservationDate(date);
        e.setTimeOpenAuditorium(LocalTime.of(8, 30));
        e.setReason("Palestra de teste");
        e.setStartTime(start);
        e.setEndTime(end);
        e.setUserNameRequester("Fulano de Tal");
        e.setNumPeople(50);
        e.setNecessaryScreen(true);
        e.setReservationType(ReserveType.AUDITORIUM);
        e.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        return repository.saveAndFlush(e);
    }

    /** Insere uma reserva diretamente no banco com o status informado. */
    private AuditoriumEntity persistWithStatus(ReserveStatus status) {
        AuditoriumEntity e = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        e.setStatus(status);
        return repository.saveAndFlush(e);
    }

    // ---------- createReservation ----------

    @Test
    void createReservation_startTimeNotBeforeEndTime_throws() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra", LocalTime.of(11, 0),
                LocalTime.of(9, 0), "Fulano", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);

        assertThrows(StartTimeIsNotBeforeEndTimeException.class,
                () -> service.createReservation(dto));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_invalidPeopleCount_throws() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra", LocalTime.of(9, 0),
                LocalTime.of(11, 0), "Fulano", 0, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);

        assertThrows(InvalidPeopleCountException.class,
                () -> service.createReservation(dto));
        assertEquals(0, repository.count());
    }

    @Test
        // [BLOQUEADO POR BUG A] primeira reserva do dia -> NoSuchElementException
    void createReservation_persistsWithDefaultStatusAndType() {
        service.createReservation(validDto());

        List<AuditoriumEntity> all = repository.findAll();
        assertEquals(1, all.size());

        AuditoriumEntity saved = all.getFirst();
        assertNotNull(saved.getId());
        assertEquals(ReserveType.AUDITORIUM, saved.getReservationType());
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, saved.getStatus());
        assertEquals(50, saved.getNumPeople());
        assertTrue(saved.isNecessaryScreen());
    }

    @Test
        // [BLOQUEADO POR BUG A / BUG C] a validação de horário roda antes do conflito
    void createReservation_sameDate_throwsConflict() {
        persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertThrows(ReservationConflictException.class,
                () -> service.createReservation(validDto()));
        assertEquals(1, repository.count());
    }

    // ---------- getReserveFromId / getAllReservation ----------

    @Test
    void getReserveFromId_returnsDto() {
        AuditoriumEntity saved = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));

        AuditoriumResponseDTO dto = service.getReserveFromId(saved.getId());

        assertEquals(saved.getId(), dto.id());
        assertEquals("Palestra de teste", dto.reason());
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, dto.status());
    }

    @Test
    void getReserveFromId_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.getReserveFromId(999_999L));
    }

    @Test
    void getAllReservation_returnsAll() {
        persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));

        List<AuditoriumResponseDTO> all = service.getAllReservation();

        assertEquals(1, all.size());
    }

    // ---------- approve / reject ----------

    @Test
        // [BLOQUEADO POR BUG D] validatorApprove -> findStatusById devolve a entidade
    void approve_setsStatusApproved() {
        Long id = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();

        service.approve(id);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
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
        Long id = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();

        service.reject(id, Map.of("reasonFailure", "Sala indisponível"));

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.REPROVADA, reloaded.getStatus());
        assertEquals("Sala indisponível", reloaded.getReasonFailure());
    }

    @Test
        // [BLOQUEADO POR BUG D] guarda nova isPossibleReject
    void reject_alreadyCancelled_throws() {
        Long id = persistWithStatus(ReserveStatus.CANCELADA).getId();

        assertThrows(ReservationWasCancell.class,
                () -> service.reject(id, Map.of("reasonFailure", "Sala indisponível")));
    }

    // ---------- updateReservation ----------

    @Test
    void updateReservation_updatesExisting() {
        Long id = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();

        // A validação de update (isSameReservationDate) exige uma data DIFERENTE da atual.
        AuditoriumRequestDTO update = new AuditoriumRequestDTO(
                DATE.plusDays(1), LocalTime.of(8, 30), "Reunião geral", LocalTime.of(9, 0),
                LocalTime.of(12, 0), "Fulano", 80, Boolean.TRUE,
                Boolean.FALSE, Boolean.FALSE, Boolean.TRUE);

        service.updateReservation(id, update);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(80, reloaded.getNumPeople());
        assertEquals("Reunião geral", reloaded.getReason());
        assertEquals(LocalTime.of(12, 0), reloaded.getEndTime());
        assertTrue(reloaded.isExternalPublic());
    }

    @Test
    void updateReservation_sameDate_throwsConflict() {
        Long id = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();

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
        Long id = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();

        service.deleteReservation(id);

        assertFalse(repository.existsById(id));
    }

    @Test
    void deleteReservation_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.deleteReservation(999_999L));
    }
}
