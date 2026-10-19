package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumResponseDTO;
import com.siurbinfo.srs.dto.reservation.auditorium.CoffeeBreakDTO;
import com.siurbinfo.srs.dto.reservation.cancellation.CancellationDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import com.siurbinfo.srs.dto.reservation.reason.RejectCoffeeDTO;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.InvalidPeopleCountException;
import com.siurbinfo.srs.exception.InvalidTimeBetweenReservations;
import com.siurbinfo.srs.exception.InvalidTimeException;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.ReservationWasApprove;
import com.siurbinfo.srs.exception.ReservationWasCancell;
import com.siurbinfo.srs.exception.StartTimeIsNotBeforeEndTimeException;
import com.siurbinfo.srs.exception.UserIdNotFoundedException;
import com.siurbinfo.srs.mapper.reserve.AuditoriumMapperImpl;
import com.siurbinfo.srs.repository.reservation.ReservationAuditoriumRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserServiceImpl;
import com.siurbinfo.srs.service.glpi.GlpiService;
import com.siurbinfo.srs.service.reservation.validators.AuditoriumValidator;
import com.siurbinfo.srs.service.reservation.validators.ReservationValidator;
import com.siurbinfo.srs.service.smtp.SmtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AuditoriumService.class, AuditoriumMapperImpl.class, AuditoriumValidator.class, ReservationValidator.class})
class AuditoriumServiceTest {

    @Autowired
    AuditoriumService service;

    @Autowired
    ReservationAuditoriumRepository repository;

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    CognitoUserServiceImpl cognitoUserServiceImpl;

    @MockitoBean
    GlpiService glpiService;

    @MockitoBean
    SmtpService smtpService;

    private UserEntity user;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 1);

    private static final String MOTIVO = "Evento adiado";

    private static final Jwt JWT = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .claim("sub", "aprovador")
            .build();

    private static final Jwt SOLICITANTE = jwtComGrupos(List.of("siurb-usuario"));

    @BeforeEach
    void setUpUser() {
        user = new UserEntity();
        user.setEmail("solicitante@teste.com");
        user = userRepository.saveAndFlush(user);
    }

    private UserEntity persistOtherUser() {
        UserEntity other = new UserEntity();
        other.setEmail("outro@teste.com");
        return userRepository.saveAndFlush(other);
    }

    private AuditoriumRequestDTO validDto() {
        return new AuditoriumRequestDTO(
                DATE,
                LocalTime.of(8, 30),
                "Palestra de teste",
                null,
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                "Fulano de Tal",
                "TI",
                50,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                null,
                null,
                null,
                user.getId(),
                "1140028922",
                10,
                null,
                null
        );
    }

    private AuditoriumEntity persistReservation(LocalDate date, LocalTime start, LocalTime end) {
        AuditoriumEntity e = new AuditoriumEntity();
        e.setReservationDate(date);
        e.setTimeOpenAuditorium(LocalTime.of(8, 30));
        e.setReason("Palestra de teste");
        e.setStartTime(start);
        e.setEndTime(end);
        e.setUserNameRequester("Fulano de Tal");
        e.setRequesterDepartment("TI");
        e.setNumPeople(50);
        e.setNecessaryScreen(true);
        e.setNecessaryCoffee(true);
        e.setUser(user);
        e.setReservationType(ReserveType.AUDITORIUM);
        e.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        return repository.saveAndFlush(e);
    }

    private AuditoriumEntity persistWithStatus(ReserveStatus status) {
        AuditoriumEntity e = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        e.setStatus(status);
        return repository.saveAndFlush(e);
    }

    private AuditoriumEntity persistWithCoffeeDecision(boolean approveCoffee) {
        AuditoriumEntity e = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        e.setIsApproveCoffeeBreak(approveCoffee);
        return repository.saveAndFlush(e);
    }

    private AuditoriumEntity persistWithoutCoffee() {
        AuditoriumEntity e = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        e.setNecessaryCoffee(false);
        return repository.saveAndFlush(e);
    }

    @Test
    void createReservation_startTimeNotBeforeEndTime_throws() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra", null, LocalTime.of(11, 0),
                LocalTime.of(9, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, null, null, null, null, null);

        assertThrows(StartTimeIsNotBeforeEndTimeException.class,
                () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_invalidPeopleCount_throws() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra", null, LocalTime.of(9, 0),
                LocalTime.of(11, 0), "Fulano", "TI", 0, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, null, null, null, null, null);

        assertThrows(InvalidPeopleCountException.class,
                () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_tokenSemClaimDeGrupos_lancaSecurityException() {
        assertThrows(SecurityException.class, () -> service.createReservation(validDto(), JWT));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_tokenComGruposVazio_lancaSecurityException() {
        assertThrows(SecurityException.class,
                () -> service.createReservation(validDto(), jwtComGrupos(List.of())));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_persistsWithDefaultStatusAndType() {
        service.createReservation(validDto(), SOLICITANTE);

        List<AuditoriumEntity> all = repository.findAll();
        assertEquals(1, all.size());

        AuditoriumEntity saved = all.getFirst();
        assertNotNull(saved.getId());
        assertEquals(ReserveType.AUDITORIUM, saved.getReservationType());
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, saved.getStatus());
        assertEquals(50, saved.getNumPeople());
        assertEquals("TI", saved.getRequesterDepartment());
        assertTrue(saved.isNecessaryScreen());
    }

    @Test
    void createReservation_overlappingApprovedReservation_throwsConflict() {
        persistWithStatus(ReserveStatus.APROVADA);

        assertThrows(ReservationConflictException.class,
                () -> service.createReservation(validDto(), SOLICITANTE));
        assertEquals(1, repository.count());
    }

    @Test
    void createReservation_overlappingPendingReservation_isAllowed() {
        persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));

        service.createReservation(validDto(), SOLICITANTE);

        assertEquals(2, repository.count());
    }

    @Test
    void createReservation_lessThanMinGapFromPreviousApproved_throwsInvalidTime() {
        persistWithStatus(ReserveStatus.APROVADA);

        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra", null, LocalTime.of(11, 20),
                LocalTime.of(12, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, null, null, null, null, null);

        assertThrows(InvalidTimeBetweenReservations.class,
                () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(1, repository.count());
    }

    @Test
    void createReservation_sufficientGapFromPreviousApproved_succeeds() {
        persistWithStatus(ReserveStatus.APROVADA);

        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra", null, LocalTime.of(11, 40),
                LocalTime.of(12, 30), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        service.createReservation(dto, SOLICITANTE);

        assertEquals(2, repository.count());
    }

    @Test
    void createReservation_usuarioEspecial_ignoraConflitoComAprovada() {
        persistWithStatus(ReserveStatus.APROVADA);

        service.createReservation(validDto(), jwtComGrupos(List.of("siurb-usuario-geral-especial")));

        assertEquals(2, repository.count());
    }

    @Test
    void createReservation_usuarioEspecial_ignoraIntervaloMinimo() {
        persistWithStatus(ReserveStatus.APROVADA);

        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra", null, LocalTime.of(11, 20),
                LocalTime.of(12, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        service.createReservation(dto, jwtComGrupos(List.of("siurb-usuario-geral-especial")));

        assertEquals(2, repository.count());
    }

    @Test
    void getReserveFromId_returnsDto() {
        AuditoriumEntity saved = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));

        AuditoriumResponseDTO dto = service.getReserveFromId(saved.getId());

        assertEquals(saved.getId(), dto.id());
        assertEquals("Palestra de teste", dto.reason());
        assertEquals("TI", dto.requesterDepartment());
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, dto.status());
    }

    @Test
    void getReserveFromId_retornaCoffeeBreakAgrupado() {
        AuditoriumEntity saved = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        saved.setReasonCoffee("Coffee para 50");
        saved.setFoodSetupTime(LocalTime.of(10, 0));
        saved.setServiceTime(LocalTime.of(10, 15));
        saved.setIsApproveCoffeeBreak(false);
        saved.setReasonRejectCoffee("Sem verba");
        repository.saveAndFlush(saved);

        CoffeeBreakDTO coffee = service.getReserveFromId(saved.getId()).coffeeBreak();

        assertNotNull(coffee);
        assertTrue(coffee.necessaryCoffee());
        assertEquals("Coffee para 50", coffee.reasonCoffee());
        assertEquals(LocalTime.of(10, 0), coffee.foodSetupTime());
        assertEquals(LocalTime.of(10, 15), coffee.serviceTime());
        assertFalse(coffee.isApproveCoffeeBreak());
        assertEquals("Sem verba", coffee.reasonRejectCoffee());
    }

    @Test
    void createReservation_comCoffee_persisteHorariosDeMontagemEServico() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra", null, LocalTime.of(9, 0),
                LocalTime.of(11, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, LocalTime.of(10, 0), LocalTime.of(10, 15),
                "Coffee para 50", user.getId(), null, null, null, null);

        service.createReservation(dto, SOLICITANTE);

        AuditoriumEntity saved = repository.findAll().getFirst();
        assertTrue(saved.isNecessaryCoffee());
        assertEquals(LocalTime.of(10, 0), saved.getFoodSetupTime());
        assertEquals(LocalTime.of(10, 15), saved.getServiceTime());
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

    @Test
    void approve_setsStatusApproved() {
        Long id = persistWithCoffeeDecision(true).getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.approve(id, JWT);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.APROVADA, reloaded.getStatus());
        assertEquals("Aprovador Teste", reloaded.getUserNameApprover());
    }

    @Test
    void approve_alreadyApproved_throws() {
        Long id = persistWithStatus(ReserveStatus.APROVADA).getId();

        assertThrows(ReservationWasApprove.class, () -> service.approve(id, JWT));
    }

    @Test
    void approve_coffeeNotDecidedYet_throwsConflict() {
        Long id = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();

        assertThrows(ReservationConflictException.class, () -> service.approve(id, JWT));

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, reloaded.getStatus());
    }

    @Test
    void approve_noCoffeeRequested_approvesWithoutCoffeeDecision() {
        Long id = persistWithoutCoffee().getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.approve(id, JWT);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.APROVADA, reloaded.getStatus());
        assertNull(reloaded.getIsApproveCoffeeBreak());
    }

    @Test
    void approveCoffee_marksCoffeeAsApproved() {
        Long id = persistWithCoffeeDecision(false).getId();

        service.approveCoffee(id);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(Boolean.TRUE, reloaded.getIsApproveCoffeeBreak());
        assertNull(reloaded.getReasonRejectCoffee());
    }

    @Test
    void rejectCoffee_marksCoffeeAsRejectedWithReason() {
        Long id = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();

        service.rejectCoffee(id, new RejectCoffeeDTO("Sem verba para o coffee"));

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(Boolean.FALSE, reloaded.getIsApproveCoffeeBreak());
        assertEquals("Sem verba para o coffee", reloaded.getReasonRejectCoffee());
    }

    @Test
    void reject_setsStatusRejectedWithReason() {
        Long id = persistWithCoffeeDecision(false).getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.reject(id, Map.of("reasonFailure", "Sala indisponível"), JWT);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.REPROVADA, reloaded.getStatus());
        assertEquals("Sala indisponível", reloaded.getReasonFailure());
        assertEquals("Aprovador Teste", reloaded.getUserNameApprover());
    }

    @Test
    void reject_alreadyCancelled_throws() {
        Long id = persistWithStatus(ReserveStatus.CANCELADA).getId();

        assertThrows(ReservationWasCancell.class,
                () -> service.reject(id, Map.of("reasonFailure", "Sala indisponível"), JWT));
    }

    @Test
    void reject_coffeeNotDecidedYet_throwsConflict() {
        Long id = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();

        assertThrows(ReservationConflictException.class,
                () -> service.reject(id, Map.of("reasonFailure", "Sala indisponível"), JWT));
    }

    @Test
    void updateReservation_updatesExisting() {
        Long id = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();

        AuditoriumRequestDTO update = new AuditoriumRequestDTO(
                DATE.plusDays(1), LocalTime.of(8, 30), "Reunião geral", null, LocalTime.of(9, 0),
                LocalTime.of(12, 0), "Fulano", "RH", 80, Boolean.TRUE,
                Boolean.FALSE, Boolean.FALSE, Boolean.TRUE, LocalTime.of(11, 30), LocalTime.of(11, 40),
                "Coffee para 80 pessoas", null, null, null, null, null);

        service.updateReservation(id, update);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(80, reloaded.getNumPeople());
        assertEquals("Reunião geral", reloaded.getReason());
        assertEquals("RH", reloaded.getRequesterDepartment());
        assertEquals(LocalTime.of(12, 0), reloaded.getEndTime());
        assertTrue(reloaded.isExternalPublic());
    }

    @Test
    void updateReservation_overlappingApprovedReservation_throwsConflict() {
        Long id = persistReservation(DATE, LocalTime.of(14, 0), LocalTime.of(15, 0)).getId();
        persistWithStatus(ReserveStatus.APROVADA);

        assertThrows(ReservationConflictException.class,
                () -> service.updateReservation(id, validDto()));
    }

    @Test
    void updateReservation_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.updateReservation(999_999L, validDto()));
    }

    @Test
    void createReservation_persistsUserTelephoneAndExternalPeople() {
        service.createReservation(validDto(), SOLICITANTE);

        AuditoriumEntity saved = repository.findAll().getFirst();
        assertNotNull(saved.getUser());
        assertEquals(user.getId(), saved.getUser().getId());
        assertEquals("1140028922", saved.getTelephone());
        assertEquals(10, saved.getNumExternalPeople());
    }

    @Test
    void createReservation_unknownUserId_throws() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra", null, LocalTime.of(9, 0),
                LocalTime.of(11, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null,
                999_999L, "1140028922", 10, null, null);

        assertThrows(UserIdNotFoundedException.class, () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_semUserId_lancaUserIdNotFounded() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra", null, LocalTime.of(9, 0),
                LocalTime.of(11, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null,
                null, null, null, null, null);

        assertThrows(UserIdNotFoundedException.class, () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void getReserveFromId_returnsNewFields() {
        AuditoriumResponseDTO created = service.createReservation(validDto(), SOLICITANTE);

        AuditoriumResponseDTO dto = service.getReserveFromId(created.id());

        assertEquals(user.getId(), dto.userId());
        assertEquals("1140028922", dto.telephone());
        assertEquals(10, dto.numExternalPeople());
    }

    @Test
    void updateReservation_changesUserAndNewFields() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();
        UserEntity outro = persistOtherUser();

        AuditoriumRequestDTO update = new AuditoriumRequestDTO(
                DATE.plusDays(1), LocalTime.of(8, 30), "Palestra", null, LocalTime.of(14, 0),
                LocalTime.of(16, 0), "Fulano", "TI", 50, Boolean.TRUE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null,
                outro.getId(), "1130001111", 25, null, null);

        service.updateReservation(id, update);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(outro.getId(), reloaded.getUser().getId());
        assertEquals("1130001111", reloaded.getTelephone());
        assertEquals(25, reloaded.getNumExternalPeople());
    }

    @Test
    void updateReservation_withoutUserId_keepsCurrentUser() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        AuditoriumRequestDTO update = new AuditoriumRequestDTO(
                DATE.plusDays(1), LocalTime.of(8, 30), "Palestra", null, LocalTime.of(14, 0),
                LocalTime.of(16, 0), "Fulano", "TI", 50, Boolean.TRUE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null,
                null, "1130001111", 25, null, null);

        service.updateReservation(id, update);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertNotNull(reloaded.getUser());
        assertEquals(user.getId(), reloaded.getUser().getId());
    }

    private AuditoriumRequestDTO dtoComObservacao(String observacao) {
        return new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra de teste", null, LocalTime.of(9, 0),
                LocalTime.of(11, 0), "Fulano de Tal", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null,
                user.getId(), "1140028922", 10, observacao, null);
    }

    @Test
    void createReservation_persistsObservation() {
        service.createReservation(dtoComObservacao("Precisa de 2 microfones sem fio"), SOLICITANTE);

        AuditoriumEntity saved = repository.findAll().getFirst();
        assertEquals("Precisa de 2 microfones sem fio", saved.getObservation());
    }

    @Test
    void createReservation_withoutObservation_savesNull() {
        service.createReservation(validDto(), SOLICITANTE);

        AuditoriumEntity saved = repository.findAll().getFirst();
        assertNull(saved.getObservation());
    }

    @Test
    void updateReservation_changesObservation() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        AuditoriumRequestDTO update = new AuditoriumRequestDTO(
                DATE.plusDays(1), LocalTime.of(8, 30), "Palestra", null, LocalTime.of(14, 0),
                LocalTime.of(16, 0), "Fulano", "TI", 50, Boolean.TRUE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null,
                user.getId(), "1130001111", 25, "Montar o palco até as 8h", null);

        service.updateReservation(id, update);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals("Montar o palco até as 8h", reloaded.getObservation());
    }

    @Test
    void getReserveFromId_returnsObservation() {
        AuditoriumResponseDTO created =
                service.createReservation(dtoComObservacao("Levar cadeiras extras"), SOLICITANTE);

        AuditoriumResponseDTO dto = service.getReserveFromId(created.id());

        assertEquals("Levar cadeiras extras", dto.observation());
    }

    private AuditoriumRequestDTO dtoComMotivoAbertura(String motivo) {
        return new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra de teste", null, LocalTime.of(9, 0),
                LocalTime.of(11, 0), "Fulano de Tal", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null,
                user.getId(), "1140028922", 10, null, motivo);
    }

    @Test
    void createReservation_persistsReasonOpenAuditorium() {
        service.createReservation(dtoComMotivoAbertura("Montagem do palco e passagem de som"), SOLICITANTE);

        AuditoriumEntity saved = repository.findAll().getFirst();
        assertEquals("Montagem do palco e passagem de som", saved.getReasonOpenAuditorium());
    }

    @Test
    void createReservation_withoutReasonOpenAuditorium_savesNull() {
        service.createReservation(validDto(), SOLICITANTE);

        AuditoriumEntity saved = repository.findAll().getFirst();
        assertNull(saved.getReasonOpenAuditorium());
    }

    @Test
    void createReservation_reasonOpenAuditorium_naoMisturaComObservation() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(8, 30), "Palestra de teste", null, LocalTime.of(9, 0),
                LocalTime.of(11, 0), "Fulano de Tal", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null,
                user.getId(), "1140028922", 10, "Levar cadeiras extras", "Credenciamento dos convidados");

        service.createReservation(dto, SOLICITANTE);

        AuditoriumEntity saved = repository.findAll().getFirst();
        assertEquals("Levar cadeiras extras", saved.getObservation());
        assertEquals("Credenciamento dos convidados", saved.getReasonOpenAuditorium());
    }

    @Test
    void updateReservation_changesReasonOpenAuditorium() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        AuditoriumRequestDTO update = new AuditoriumRequestDTO(
                DATE.plusDays(1), LocalTime.of(8, 30), "Palestra", null, LocalTime.of(14, 0),
                LocalTime.of(16, 0), "Fulano", "TI", 50, Boolean.TRUE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null,
                user.getId(), "1130001111", 25, null, "Ensaio da banda antes do evento");

        service.updateReservation(id, update);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals("Ensaio da banda antes do evento", reloaded.getReasonOpenAuditorium());
    }

    @Test
    void getReserveFromId_returnsReasonOpenAuditorium() {
        AuditoriumResponseDTO created =
                service.createReservation(dtoComMotivoAbertura("Montagem do palco"), SOLICITANTE);

        AuditoriumResponseDTO dto = service.getReserveFromId(created.id());

        assertEquals("Montagem do palco", dto.reasonOpenAuditorium());
    }

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

    @Test
    void approve_abreChamadoNoGlpi() {
        Long id = persistWithCoffeeDecision(true).getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.approve(id, JWT);

        verify(glpiService).createTicket(id);
    }

    @Test
    void approve_reservaJaAprovada_naoAbreOutroChamado() {
        Long id = persistWithStatus(ReserveStatus.APROVADA).getId();

        assertThrows(ReservationWasApprove.class, () -> service.approve(id, JWT));

        verify(glpiService, never()).createTicket(anyLong());
    }

    @Test
    void reject_naoAbreChamadoNoGlpi() {
        Long id = persistWithCoffeeDecision(true).getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.reject(id, Map.of("reasonFailure", "Sem estrutura"), JWT);

        verify(glpiService, never()).createTicket(anyLong());
    }

    @Test
    void getReservationFromYearAndMonth_trazSoOMesEAnoPedidos() {
        Long doMes = persistReservation(LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();
        persistReservation(LocalDate.of(2026, 9, 10), LocalTime.of(9, 0), LocalTime.of(11, 0));
        persistReservation(LocalDate.of(2025, 8, 10), LocalTime.of(9, 0), LocalTime.of(11, 0));

        List<AuditoriumResponseDTO> encontradas = service.getReservationFromYearAndMonth(2026, 8);

        assertEquals(1, encontradas.size());
        assertEquals(doMes, encontradas.get(0).id());
    }

    @Test
    void getReservationFromYearAndMonth_semReservaNoMes_retornaVazio() {
        persistReservation(LocalDate.of(2026, 8, 10), LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertTrue(service.getReservationFromYearAndMonth(2026, 12).isEmpty());
    }

    private static Jwt jwtComGrupos(List<String> grupos) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "solicitante")
                .claim("cognito:groups", grupos)
                .build();
    }

    private static UserResponseDTO userDto(UserEntity u) {
        return new UserResponseDTO(u.getId(), u.getEmail(), null, null, Map.of(), false, List.of(), null, false);
    }

    private AuditoriumEntity persistWithUserAndStatus(UserEntity dono, ReserveStatus status) {
        AuditoriumEntity e = persistReservation(DATE, LocalTime.of(9, 0), LocalTime.of(11, 0));
        e.setUser(dono);
        e.setStatus(status);
        return repository.saveAndFlush(e);
    }

    @Test
    void cancelReservation_semJwt_lancaSecurityException() {
        Long id = persistWithUserAndStatus(user, ReserveStatus.ENVIADA_PARA_ANALISE).getId();

        assertThrows(SecurityException.class, () -> service.cancelReservation(new CancellationDTO(id, MOTIVO), null));

        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, repository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void cancelReservation_donoDaReserva_marcaComoCancelada() {
        Long id = persistWithUserAndStatus(user, ReserveStatus.ENVIADA_PARA_ANALISE).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-usuario"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt);

        assertEquals(ReserveStatus.CANCELADA, repository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void cancelReservation_reservaJaCancelada_lancaReservationWasCancell() {
        Long id = persistWithUserAndStatus(user, ReserveStatus.CANCELADA).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-usuario"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        assertThrows(ReservationWasCancell.class, () -> service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt));
    }

    @Test
    void cancelReservation_outroUsuarioSemPrivilegio_lancaSecurityExceptionEMantemStatus() {
        UserEntity outro = persistOtherUser();
        Long id = persistWithUserAndStatus(outro, ReserveStatus.ENVIADA_PARA_ANALISE).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-usuario"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        assertThrows(SecurityException.class, () -> service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt));

        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, repository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void cancelReservation_administrador_cancelaReservaDeOutroUsuario() {
        UserEntity outro = persistOtherUser();
        Long id = persistWithUserAndStatus(outro, ReserveStatus.ENVIADA_PARA_ANALISE).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-administrador"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt);

        AuditoriumEntity cancelada = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.CANCELADA, cancelada.getStatus());
        assertEquals(user.getId(), cancelada.getCanceledBy().getId(),
                "canceled_by gravou o dono da reserva no lugar de quem cancelou");
        assertNotEquals(outro.getId(), cancelada.getCanceledBy().getId());
    }

    @Test
    void cancelReservation_divisaoAdministrativa_cancelaReservaDeOutroUsuario() {
        UserEntity outro = persistOtherUser();
        Long id = persistWithUserAndStatus(outro, ReserveStatus.ENVIADA_PARA_ANALISE).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-divisao-administrativa"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt);

        assertEquals(ReserveStatus.CANCELADA, repository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void cancelReservation_reservaJaAprovada_eCancelada() {
        Long id = persistWithUserAndStatus(user, ReserveStatus.APROVADA).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-usuario"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt);

        assertEquals(ReserveStatus.CANCELADA, repository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void cancelReservation_idInexistente_lancaReservationIdIsNotFounded() {
        Jwt jwt = jwtComGrupos(List.of("siurb-usuario"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.cancelReservation(new CancellationDTO(999_999L, MOTIVO), jwt));
    }

    @Test
    void cancelReservation_gravaOMotivoEAHoraDoCancelamento() {
        Long id = persistWithUserAndStatus(user, ReserveStatus.ENVIADA_PARA_ANALISE).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-usuario"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt);

        AuditoriumEntity cancelada = repository.findById(id).orElseThrow();
        assertEquals(MOTIVO, cancelada.getReasonsCancellation());
        assertNotNull(cancelada.getDateCancellation(), "o cancelamento ficou sem data");
        assertEquals(user.getId(), cancelada.getCanceledBy().getId(),
                "canceled_by nao gravou quem cancelou");
    }

    @Test
    void cancelReservation_avisaOSolicitantePorEmail() {
        Long id = persistWithUserAndStatus(user, ReserveStatus.ENVIADA_PARA_ANALISE).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-usuario"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt);

        verify(smtpService).sendEmail(any(AuditoriumEntity.class));
    }

    @Test
    void createReservation_avisaOSolicitantePorEmail() {
        service.createReservation(validDto(), SOLICITANTE);

        verify(smtpService).sendEmail(any(AuditoriumEntity.class));
    }

    @Test
    void approve_avisaOSolicitantePorEmail() {
        Long id = persistWithCoffeeDecision(true).getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.approve(id, JWT);

        verify(smtpService).sendEmail(any(AuditoriumEntity.class));
    }

    @Test
    void reject_avisaOSolicitantePorEmail() {
        Long id = persistWithCoffeeDecision(false).getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.reject(id, Map.of("reasonFailure", "Sala indisponível"), JWT);

        verify(smtpService).sendEmail(any(AuditoriumEntity.class));
    }

    @Test
    void createReservation_timeOpenAuditoriumAntesDasSete_lancaInvalidTime() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(6, 0), "Palestra", null, LocalTime.of(9, 0),
                LocalTime.of(11, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                () -> service.createReservation(dto, SOLICITANTE));

        assertEquals("O horário precisa ser depois das 7 da manhã", ex.getMessage());
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_startTimeAntesDasSete_lancaInvalidTime() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(7, 0), "Palestra", null, LocalTime.of(6, 30),
                LocalTime.of(11, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        assertThrows(InvalidTimeException.class, () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_abrindoAsSeteEmPonto_persiste() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(7, 0), "Palestra", null, LocalTime.of(7, 0),
                LocalTime.of(11, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        assertDoesNotThrow(() -> service.createReservation(dto, SOLICITANTE));
        assertEquals(1, repository.count());
    }

    @Test
    void createReservation_terminaDepoisDas19SemMotivo_lancaInvalidTime() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(17, 0), "Palestra", null, LocalTime.of(18, 0),
                LocalTime.of(19, 30), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                () -> service.createReservation(dto, SOLICITANTE));

        assertEquals("É necessário o motivo para reservas depois das 19h", ex.getMessage());
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_terminaDepoisDas19ComMotivoEmBranco_lancaInvalidTime() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(17, 0), "Palestra", "   ", LocalTime.of(18, 0),
                LocalTime.of(19, 30), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        assertThrows(InvalidTimeException.class, () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_terminaDepoisDas19ComMotivo_persisteOMotivo() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(17, 0), "Palestra", "Evento com público externo à noite",
                LocalTime.of(18, 0), LocalTime.of(19, 30), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        service.createReservation(dto, SOLICITANTE);

        AuditoriumEntity saved = repository.findAll().getFirst();
        assertEquals("Evento com público externo à noite", saved.getReasonOutsideHours());
    }

    @Test
    void createReservation_terminandoAs19EmPonto_naoExigeMotivo() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(17, 0), "Palestra", null, LocalTime.of(18, 0),
                LocalTime.of(19, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        assertDoesNotThrow(() -> service.createReservation(dto, SOLICITANTE));

        AuditoriumEntity saved = repository.findAll().getFirst();
        assertNull(saved.getReasonOutsideHours());
    }

    @Test
    void createReservation_dentroDoHorario_naoGravaMotivo() {
        service.createReservation(validDto(), SOLICITANTE);

        AuditoriumEntity saved = repository.findAll().getFirst();
        assertNull(saved.getReasonOutsideHours());
    }

    @Test
    void getReserveFromId_retornaReasonOutsideHours() {
        AuditoriumRequestDTO dto = new AuditoriumRequestDTO(
                DATE, LocalTime.of(17, 0), "Palestra", "Montagem noturna do palco",
                LocalTime.of(18, 0), LocalTime.of(20, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);
        Long id = service.createReservation(dto, SOLICITANTE).id();

        AuditoriumResponseDTO response = service.getReserveFromId(id);

        assertEquals("Montagem noturna do palco", response.reasonOutsideHours());
    }

    @Test
    void updateReservation_timeOpenAuditoriumAntesDasSete_lancaInvalidTime() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        AuditoriumRequestDTO update = new AuditoriumRequestDTO(
                DATE.plusDays(1), LocalTime.of(6, 0), "Palestra", null, LocalTime.of(9, 0),
                LocalTime.of(11, 0), "Fulano", "TI", 50, Boolean.TRUE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                () -> service.updateReservation(id, update));

        assertEquals("O horário precisa ser depois das 7 da manhã", ex.getMessage());
        assertEquals(LocalTime.of(8, 30), repository.findById(id).orElseThrow().getTimeOpenAuditorium());
    }

    @Test
    void updateReservation_startTimeAntesDasSete_lancaInvalidTime() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        AuditoriumRequestDTO update = new AuditoriumRequestDTO(
                DATE.plusDays(1), LocalTime.of(7, 0), "Palestra", null, LocalTime.of(6, 30),
                LocalTime.of(11, 0), "Fulano", "TI", 50, Boolean.TRUE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        assertThrows(InvalidTimeException.class, () -> service.updateReservation(id, update));
        assertEquals(LocalTime.of(9, 0), repository.findById(id).orElseThrow().getStartTime());
    }

    @Test
    void updateReservation_terminaDepoisDas19SemMotivo_lancaInvalidTime() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        AuditoriumRequestDTO update = new AuditoriumRequestDTO(
                DATE.plusDays(1), LocalTime.of(17, 0), "Palestra", null, LocalTime.of(18, 0),
                LocalTime.of(19, 30), "Fulano", "TI", 50, Boolean.TRUE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                () -> service.updateReservation(id, update));

        assertEquals("É necessário o motivo para reservas depois das 19h", ex.getMessage());
        assertEquals(LocalTime.of(11, 0), repository.findById(id).orElseThrow().getEndTime());
    }

    @Test
    void updateReservation_terminaDepoisDas19ComMotivo_persisteOMotivo() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        AuditoriumRequestDTO update = new AuditoriumRequestDTO(
                DATE.plusDays(1), LocalTime.of(17, 0), "Palestra", "Evento noturno autorizado",
                LocalTime.of(18, 0), LocalTime.of(19, 30), "Fulano", "TI", 50, Boolean.TRUE,
                Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, null, null, null, user.getId(), null, null, null, null);

        service.updateReservation(id, update);

        AuditoriumEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals("Evento noturno autorizado", reloaded.getReasonOutsideHours());
        assertEquals(LocalTime.of(19, 30), reloaded.getEndTime());
    }
}
