package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.cancellation.CancellationDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomRequestDTO;
import com.siurbinfo.srs.dto.reservation.meetingRoom.MeetingRoomResponseDTO;
import com.siurbinfo.srs.dto.reservation.reason.RejectCoffeeDTO;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.entity.room.RoomEntity;
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
import com.siurbinfo.srs.exception.RoomIdIsNotFoundedException;
import com.siurbinfo.srs.exception.StartTimeIsNotBeforeEndTimeException;
import com.siurbinfo.srs.exception.UserIdNotFoundedException;
import com.siurbinfo.srs.mapper.reserve.MeetingMapperImpl;
import com.siurbinfo.srs.repository.reservation.ReservationMeetingRoomRepository;
import com.siurbinfo.srs.repository.room.RoomRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserServiceImpl;
import com.siurbinfo.srs.service.reservation.validators.MeetingRoomValidator;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    CognitoUserServiceImpl cognitoUserServiceImpl;

    @MockitoBean
    SmtpService smtpService;

    private RoomEntity room;

    private UserEntity user;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 1);

    private static final String MOTIVO = "Evento adiado";

    private static final Jwt JWT = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .claim("sub", "aprovador")
            .build();

    private static final Jwt SOLICITANTE = jwtComGrupos(List.of("siurb-usuario"));

    @BeforeEach
    void setUp() {
        room = new RoomEntity();
        room.setFloor("ANDAR TESTE");
        room = roomRepository.save(room);

        user = new UserEntity();
        user.setEmail("solicitante@teste.com");
        user = userRepository.saveAndFlush(user);
    }

    private UserEntity persistOtherUser() {
        UserEntity other = new UserEntity();
        other.setEmail("outro@teste.com");
        return userRepository.saveAndFlush(other);
    }

    private MeetingRoomRequestDTO validDto() {
        return new MeetingRoomRequestDTO(
                DATE,
                room.getId(),
                LocalTime.of(9, 0),
                null,
                "Fulano de Tal",
                "TI",
                LocalTime.of(10, 0),
                5,
                Boolean.TRUE,
                "Coffee para 5",
                "Reunião de teste",
                user.getId(),
                "1140028922"
        );
    }

    private MeetingRoomEntity persistReservation(LocalTime start, LocalTime end) {
        MeetingRoomEntity e = new MeetingRoomEntity();
        e.setReservationDate(DATE);
        e.setRoom(room);
        e.setStartTime(start);
        e.setEndTime(end);
        e.setUserNameRequester("Fulano de Tal");
        e.setRequesterDepartment("TI");
        e.setNumPeople(5);
        e.setIsCoffee(Boolean.TRUE);
        e.setUser(user);
        e.setReservationType(ReserveType.MEETING_ROOM);
        e.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        return repository.saveAndFlush(e);
    }

    private MeetingRoomEntity persistReservationEm(LocalDate date) {
        MeetingRoomEntity e = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0));
        e.setReservationDate(date);
        return repository.saveAndFlush(e);
    }

    private MeetingRoomEntity persistWithStatus(ReserveStatus status) {
        MeetingRoomEntity e = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0));
        e.setStatus(status);
        return repository.saveAndFlush(e);
    }

    private MeetingRoomEntity persistWithCoffeeDecision(boolean approveCoffee) {
        MeetingRoomEntity e = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0));
        e.setIsApproveCoffee(approveCoffee);
        return repository.saveAndFlush(e);
    }

    private MeetingRoomEntity persistWithoutCoffee() {
        MeetingRoomEntity e = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0));
        e.setIsCoffee(Boolean.FALSE);
        return repository.saveAndFlush(e);
    }

    @Test
    void createReservation_startTimeNotBeforeEndTime_throws() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(10, 0), null, "Fulano", "TI",
                LocalTime.of(9, 0), 5, Boolean.FALSE, null, null, null, null);

        assertThrows(StartTimeIsNotBeforeEndTimeException.class,
                () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_invalidPeopleCount_throws() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(9, 0), null, "Fulano", "TI",
                LocalTime.of(10, 0), 0, Boolean.FALSE, null, null, null, null);

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

        List<MeetingRoomEntity> all = repository.findAll();
        assertEquals(1, all.size());

        MeetingRoomEntity saved = all.getFirst();
        assertNotNull(saved.getId());
        assertEquals(ReserveType.MEETING_ROOM, saved.getReservationType());
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, saved.getStatus());
        assertEquals(room.getId(), saved.getRoom().getId());
        assertEquals(5, saved.getNumPeople());
        assertEquals("TI", saved.getRequesterDepartment());
        assertTrue(saved.getIsCoffee());
    }

    @Test
    void createReservation_roomNotFound_throws() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, 999_999L, LocalTime.of(9, 0), null, "Fulano", "TI",
                LocalTime.of(10, 0), 5, Boolean.FALSE, null, null, null, null);

        assertThrows(RoomIdIsNotFoundedException.class,
                () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_sameRoomAndDateWithApprovedReservation_throwsConflict() {
        persistWithStatus(ReserveStatus.APROVADA);

        assertThrows(ReservationConflictException.class,
                () -> service.createReservation(validDto(), SOLICITANTE));
        assertEquals(1, repository.count());
    }

    @Test
    void createReservation_sameRoomAndDateWithPendingReservation_isAllowed() {
        persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0));

        service.createReservation(validDto(), SOLICITANTE);

        assertEquals(2, repository.count());
    }

    @Test
    void createReservation_lessThanMinGapFromPreviousApproved_throwsInvalidTime() {
        persistWithStatus(ReserveStatus.APROVADA);

        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(10, 10), null, "Fulano", "TI",
                LocalTime.of(11, 0), 5, Boolean.FALSE, null, null, null, null);

        assertThrows(InvalidTimeBetweenReservations.class,
                () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(1, repository.count());
    }

    @Test
    void createReservation_sufficientGapFromPreviousApproved_succeeds() {
        persistWithStatus(ReserveStatus.APROVADA);

        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(10, 20), null, "Fulano", "TI",
                LocalTime.of(11, 0), 5, Boolean.TRUE, null, null, user.getId(), null);

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

        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(10, 10), null, "Fulano", "TI",
                LocalTime.of(11, 0), 5, Boolean.FALSE, null, null, user.getId(), null);

        service.createReservation(dto, jwtComGrupos(List.of("siurb-usuario-geral-especial")));

        assertEquals(2, repository.count());
    }

    @Test
    void createReservation_usuarioEspecial_aindaValidaHorarioInicialAntesDoFinal() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(11, 0), null, "Fulano", "TI",
                LocalTime.of(10, 0), 5, Boolean.FALSE, null, null, user.getId(), null);

        assertThrows(StartTimeIsNotBeforeEndTimeException.class,
                () -> service.createReservation(dto, jwtComGrupos(List.of("siurb-usuario-geral-especial"))));
        assertEquals(0, repository.count());
    }

    @Test
    void getReservationFromId_returnsDto() {
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        MeetingRoomResponseDTO dto = service.getReservationFromId(id);

        assertEquals(id, dto.id());
        assertNotNull(dto.room());
        assertEquals("TI", dto.requesterDepartment());
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

    @Test
    void approve_setsStatusApproved() {
        Long id = persistWithCoffeeDecision(true).getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.approve(id, JWT);

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
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
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        assertThrows(ReservationConflictException.class, () -> service.approve(id, JWT));

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, reloaded.getStatus());
    }

    @Test
    void approve_noCoffeeRequested_approvesWithoutCoffeeDecision() {
        Long id = persistWithoutCoffee().getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.approve(id, JWT);

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.APROVADA, reloaded.getStatus());
        assertNull(reloaded.getIsApproveCoffee());
    }

    @Test
    void approveCoffee_marksCoffeeAsApproved() {
        Long id = persistWithCoffeeDecision(false).getId();

        service.approveCoffee(id);

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(Boolean.TRUE, reloaded.getIsApproveCoffee());
        assertNull(reloaded.getReasonRejectCoffee());
    }

    @Test
    void rejectCoffee_marksCoffeeAsRejectedWithReason() {
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        service.rejectCoffee(id, new RejectCoffeeDTO("Sem verba para o coffee"));

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(Boolean.FALSE, reloaded.getIsApproveCoffee());
        assertEquals("Sem verba para o coffee", reloaded.getReasonRejectCoffee());
    }

    @Test
    void reject_setsStatusRejectedWithReason() {
        Long id = persistWithCoffeeDecision(false).getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.reject(id, Map.of("reasonFailure", "Conflito de agenda"), JWT);

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.REPROVADA, reloaded.getStatus());
        assertEquals("Conflito de agenda", reloaded.getReasonFailure());
        assertEquals("Aprovador Teste", reloaded.getUserNameApprover());
    }

    @Test
    void reject_alreadyCancelled_throws() {
        Long id = persistWithStatus(ReserveStatus.CANCELADA).getId();

        assertThrows(ReservationWasCancell.class,
                () -> service.reject(id, Map.of("reasonFailure", "Conflito de agenda"), JWT));
    }

    @Test
    void reject_coffeeNotDecidedYet_throwsConflict() {
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        assertThrows(ReservationConflictException.class,
                () -> service.reject(id, Map.of("reasonFailure", "Conflito de agenda"), JWT));
    }

    @Test
    void updateReservation_updatesExisting() {
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        MeetingRoomRequestDTO update = new MeetingRoomRequestDTO(
                DATE.plusDays(1), room.getId(), LocalTime.of(9, 0), null, "Fulano", "RH",
                LocalTime.of(11, 0), 8, Boolean.FALSE, null, "Atualizada", null, null);

        service.updateReservation(id, update);

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(8, reloaded.getNumPeople());
        assertEquals("RH", reloaded.getRequesterDepartment());
        assertEquals(LocalTime.of(11, 0), reloaded.getEndTime());
        assertFalse(reloaded.getIsCoffee());
    }

    @Test
    void updateReservation_sameDate_throwsConflict() {
        Long id = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        assertThrows(ReservationConflictException.class,
                () -> service.updateReservation(id, validDto()));
    }

    @Test
    void updateReservation_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.updateReservation(999_999L, validDto()));
    }

    @Test
    void createReservation_persistsUserAndTelephone() {
        service.createReservation(validDto(), SOLICITANTE);

        MeetingRoomEntity saved = repository.findAll().getFirst();
        assertNotNull(saved.getUser());
        assertEquals(user.getId(), saved.getUser().getId());
        assertEquals("1140028922", saved.getTelephone());
    }

    @Test
    void createReservation_unknownUserId_throws() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(9, 0), null, "Fulano", "TI",
                LocalTime.of(10, 0), 5, Boolean.FALSE, null, null, 999_999L, "1140028922");

        assertThrows(UserIdNotFoundedException.class, () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_semUserId_lancaUserIdNotFounded() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(9, 0), null, "Fulano", "TI",
                LocalTime.of(10, 0), 5, Boolean.FALSE, null, null, null, null);

        assertThrows(UserIdNotFoundedException.class, () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void getReservationFromId_returnsNewFields() {
        MeetingRoomResponseDTO created = service.createReservation(validDto(), SOLICITANTE);

        MeetingRoomResponseDTO dto = service.getReservationFromId(created.id());

        assertEquals(user.getId(), dto.userId());
        assertEquals("1140028922", dto.telephone());
    }

    @Test
    void updateReservation_changesUserAndTelephone() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();
        UserEntity outro = persistOtherUser();

        MeetingRoomRequestDTO update = new MeetingRoomRequestDTO(
                DATE.plusDays(1), room.getId(), LocalTime.of(14, 0), null, "Fulano", "TI",
                LocalTime.of(15, 0), 5, Boolean.FALSE, null, null, outro.getId(), "1130001111");

        service.updateReservation(id, update);

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(outro.getId(), reloaded.getUser().getId());
        assertEquals("1130001111", reloaded.getTelephone());
    }

    @Test
    void updateReservation_withoutUserId_keepsCurrentUser() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        MeetingRoomRequestDTO update = new MeetingRoomRequestDTO(
                DATE.plusDays(1), room.getId(), LocalTime.of(14, 0), null, "Fulano", "TI",
                LocalTime.of(15, 0), 5, Boolean.FALSE, null, null, null, "1130001111");

        service.updateReservation(id, update);

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertNotNull(reloaded.getUser());
        assertEquals(user.getId(), reloaded.getUser().getId());
    }

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

    @Test
    void getReservationFromYearAndMonth_trazSoOMesEAnoPedidos() {
        Long doMes = persistReservationEm(LocalDate.of(2026, 8, 10)).getId();
        persistReservationEm(LocalDate.of(2026, 9, 10));
        persistReservationEm(LocalDate.of(2025, 8, 10));

        List<MeetingRoomResponseDTO> encontradas = service.getReservationFromYearAndMonth(2026, 8);

        assertEquals(1, encontradas.size());
        assertEquals(doMes, encontradas.get(0).id());
    }

    @Test
    void getReservationFromYearAndMonth_semReservaNoMes_retornaVazio() {
        persistReservationEm(LocalDate.of(2026, 8, 10));

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

    private MeetingRoomEntity persistWithUserAndStatus(UserEntity dono, ReserveStatus status) {
        MeetingRoomEntity e = persistReservation(LocalTime.of(9, 0), LocalTime.of(10, 0));
        e.setUser(dono);
        e.setStatus(status);
        return repository.saveAndFlush(e);
    }

    @Test
    void cancelReservation_semJwt_lancaSecurityException() {
        Long id = persistWithUserAndStatus(user, ReserveStatus.ENVIADA_PARA_ANALISE).getId();

        assertThrows(SecurityException.class,
                () -> service.cancelReservation(new CancellationDTO(id, MOTIVO), null));

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
    void cancelReservation_gravaOMotivoEAHoraDoCancelamento() {
        Long id = persistWithUserAndStatus(user, ReserveStatus.ENVIADA_PARA_ANALISE).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-usuario"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt);

        MeetingRoomEntity cancelada = repository.findById(id).orElseThrow();
        assertEquals(MOTIVO, cancelada.getReasonsCancellation());
        assertNotNull(cancelada.getDateCancellation(), "o cancelamento ficou sem data");
        assertEquals(user.getId(), cancelada.getCanceledBy().getId(),
                "canceled_by nao gravou quem cancelou");
    }

    @Test
    void cancelReservation_reservaJaCancelada_lancaReservationWasCancell() {
        Long id = persistWithUserAndStatus(user, ReserveStatus.CANCELADA).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-usuario"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        assertThrows(ReservationWasCancell.class,
                () -> service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt));
    }

    @Test
    void cancelReservation_outroUsuarioSemPrivilegio_lancaSecurityExceptionEMantemStatus() {
        UserEntity outro = persistOtherUser();
        Long id = persistWithUserAndStatus(outro, ReserveStatus.ENVIADA_PARA_ANALISE).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-usuario"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        assertThrows(SecurityException.class,
                () -> service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt));

        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, repository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void cancelReservation_administrador_cancelaReservaDeOutroUsuario() {
        UserEntity outro = persistOtherUser();
        Long id = persistWithUserAndStatus(outro, ReserveStatus.ENVIADA_PARA_ANALISE).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-administrador"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt);

        MeetingRoomEntity cancelada = repository.findById(id).orElseThrow();
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
    void cancelReservation_avisaOSolicitantePorEmail() {
        Long id = persistWithUserAndStatus(user, ReserveStatus.ENVIADA_PARA_ANALISE).getId();
        Jwt jwt = jwtComGrupos(List.of("siurb-usuario"));
        given(cognitoUserServiceImpl.getUser(jwt)).willReturn(userDto(user));

        service.cancelReservation(new CancellationDTO(id, MOTIVO), jwt);

        verify(smtpService).sendEmail(any(MeetingRoomEntity.class));
    }

    @Test
    void createReservation_avisaOSolicitantePorEmail() {
        service.createReservation(validDto(), SOLICITANTE);

        verify(smtpService).sendEmail(any(MeetingRoomEntity.class));
    }

    @Test
    void approve_avisaOSolicitantePorEmail() {
        Long id = persistWithCoffeeDecision(true).getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.approve(id, JWT);

        verify(smtpService).sendEmail(any(MeetingRoomEntity.class));
    }

    @Test
    void reject_avisaOSolicitantePorEmail() {
        Long id = persistWithCoffeeDecision(false).getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.reject(id, Map.of("reasonFailure", "Conflito de agenda"), JWT);

        verify(smtpService).sendEmail(any(MeetingRoomEntity.class));
    }

    @Test
    void createReservation_startTimeAntesDasSete_lancaInvalidTime() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(6, 30), null, "Fulano", "TI",
                LocalTime.of(10, 0), 5, Boolean.FALSE, null, null, user.getId(), null);

        InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                () -> service.createReservation(dto, SOLICITANTE));

        assertEquals("O horário precisa ser depois das 7 da manhã", ex.getMessage());
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_comecandoAsSeteEmPonto_persiste() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(7, 0), null, "Fulano", "TI",
                LocalTime.of(10, 0), 5, Boolean.FALSE, null, null, user.getId(), null);

        assertDoesNotThrow(() -> service.createReservation(dto, SOLICITANTE));
        assertEquals(1, repository.count());
    }

    @Test
    void createReservation_terminaDepoisDas19SemMotivo_lancaInvalidTime() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(18, 0), null, "Fulano", "TI",
                LocalTime.of(19, 30), 5, Boolean.FALSE, null, null, user.getId(), null);

        InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                () -> service.createReservation(dto, SOLICITANTE));

        assertEquals("É necessário o motivo para reservas depois das 19h", ex.getMessage());
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_terminaDepoisDas19ComMotivoEmBranco_lancaInvalidTime() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(18, 0), "   ", "Fulano", "TI",
                LocalTime.of(19, 30), 5, Boolean.FALSE, null, null, user.getId(), null);

        assertThrows(InvalidTimeException.class, () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_terminaDepoisDas19ComMotivo_persisteOMotivo() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(18, 0), "Reunião de fechamento do mês",
                "Fulano", "TI", LocalTime.of(19, 30), 5, Boolean.FALSE, null, null, user.getId(), null);

        service.createReservation(dto, SOLICITANTE);

        MeetingRoomEntity saved = repository.findAll().getFirst();
        assertEquals("Reunião de fechamento do mês", saved.getReasonOutsideHours());
    }

    @Test
    void createReservation_terminandoAs19EmPonto_naoExigeMotivo() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(18, 0), null, "Fulano", "TI",
                LocalTime.of(19, 0), 5, Boolean.FALSE, null, null, user.getId(), null);

        assertDoesNotThrow(() -> service.createReservation(dto, SOLICITANTE));

        MeetingRoomEntity saved = repository.findAll().getFirst();
        assertNull(saved.getReasonOutsideHours());
    }

    @Test
    void createReservation_dentroDoHorario_naoGravaMotivo() {
        service.createReservation(validDto(), SOLICITANTE);

        MeetingRoomEntity saved = repository.findAll().getFirst();
        assertNull(saved.getReasonOutsideHours());
    }

    @Test
    void getReservationFromId_retornaReasonOutsideHours() {
        MeetingRoomRequestDTO dto = new MeetingRoomRequestDTO(
                DATE, room.getId(), LocalTime.of(18, 0), "Reunião com fornecedor de fora",
                "Fulano", "TI", LocalTime.of(20, 0), 5, Boolean.FALSE, null, null, user.getId(), null);
        Long id = service.createReservation(dto, SOLICITANTE).id();

        MeetingRoomResponseDTO response = service.getReservationFromId(id);

        assertEquals("Reunião com fornecedor de fora", response.reasonOutsideHours());
    }

    @Test
    void updateReservation_startTimeAntesDasSete_lancaInvalidTime() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        MeetingRoomRequestDTO update = new MeetingRoomRequestDTO(
                DATE.plusDays(1), room.getId(), LocalTime.of(6, 30), null, "Fulano", "TI",
                LocalTime.of(10, 0), 5, Boolean.FALSE, null, null, user.getId(), null);

        InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                () -> service.updateReservation(id, update));

        assertEquals("O horário precisa ser depois das 7 da manhã", ex.getMessage());
        assertEquals(LocalTime.of(9, 0), repository.findById(id).orElseThrow().getStartTime());
    }

    @Test
    void updateReservation_terminaDepoisDas19SemMotivo_lancaInvalidTime() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        MeetingRoomRequestDTO update = new MeetingRoomRequestDTO(
                DATE.plusDays(1), room.getId(), LocalTime.of(18, 0), null, "Fulano", "TI",
                LocalTime.of(19, 30), 5, Boolean.FALSE, null, null, user.getId(), null);

        InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                () -> service.updateReservation(id, update));

        assertEquals("É necessário o motivo para reservas depois das 19h", ex.getMessage());
        assertEquals(LocalTime.of(10, 0), repository.findById(id).orElseThrow().getEndTime());
    }

    @Test
    void updateReservation_terminaDepoisDas19ComMotivo_persisteOMotivo() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        MeetingRoomRequestDTO update = new MeetingRoomRequestDTO(
                DATE.plusDays(1), room.getId(), LocalTime.of(18, 0), "Reunião noturna com a diretoria",
                "Fulano", "TI", LocalTime.of(19, 30), 5, Boolean.FALSE, null, null, user.getId(), null);

        service.updateReservation(id, update);

        MeetingRoomEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals("Reunião noturna com a diretoria", reloaded.getReasonOutsideHours());
        assertEquals(LocalTime.of(19, 30), reloaded.getEndTime());
    }
}
