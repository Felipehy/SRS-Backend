package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.cancellation.CancellationDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleRequestDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleResponseDTO;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.enums.VehicleType;
import com.siurbinfo.srs.exception.InvalidTimeBetweenReservations;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.ReservationWasApprove;
import com.siurbinfo.srs.exception.ReservationWasCancell;
import com.siurbinfo.srs.exception.SameDepartureAndDestinationException;
import com.siurbinfo.srs.exception.UserIdNotFoundedException;
import com.siurbinfo.srs.mapper.reserve.VehicleMapperImpl;
import com.siurbinfo.srs.repository.reservation.ReservationFleetVehicleRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserServiceImpl;
import com.siurbinfo.srs.service.reservation.validators.ReservationValidator;
import com.siurbinfo.srs.service.reservation.validators.VehicleValidator;
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
@Import({VehicleService.class, VehicleMapperImpl.class, VehicleValidator.class, ReservationValidator.class})
class VehicleServiceTest {

    @Autowired
    VehicleService service;

    @Autowired
    ReservationFleetVehicleRepository repository;

    @Autowired
    UserRepository userRepository;

    @MockitoBean
    CognitoUserServiceImpl cognitoUserServiceImpl;

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

    private FleetVehicleRequestDTO validDto() {
        return new FleetVehicleRequestDTO(
                DATE,
                LocalTime.of(8, 0),
                "Visita técnica",
                "Sede",
                "Cliente X",
                Boolean.FALSE,
                null,
                Boolean.TRUE,
                LocalTime.of(18, 0),
                "Cliente X",
                "Sede",
                "11999998888",
                "Fulano de Tal",
                "TI",
                Boolean.FALSE,
                null,
                VehicleType.CARRO,
                3,
                user.getId(),
                "1140028922"
        );
    }

    private FleetVehicleEntity persistReservation(LocalDate date, LocalTime exitTime, String from, String to) {
        FleetVehicleEntity e = new FleetVehicleEntity();
        e.setReservationDate(date);
        e.setExitTime(exitTime);
        e.setReason("Visita técnica");
        e.setDepartureAddress(from);
        e.setDestinationAddress(to);
        e.setIsOutsideTheMunicipality(false);
        e.setUserNameRequester("Fulano de Tal");
        e.setRequesterDepartment("TI");
        e.setContact("11999998888");
        e.setNecessaryReturn(true);
        e.setReturnTime(LocalTime.of(18, 0));
        e.setNecessaryVan(false);
        e.setTypeVehicle(VehicleType.CARRO);
        e.setQuantityPassengers(3);
        e.setUser(user);
        e.setReservationType(ReserveType.FLEET_VEHICLE);
        e.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        return repository.saveAndFlush(e);
    }

    private FleetVehicleEntity persistWithStatus(ReserveStatus status) {
        FleetVehicleEntity e = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X");
        e.setStatus(status);
        return repository.saveAndFlush(e);
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
    void updateReservation_tokenSemClaimDeGrupos_lancaSecurityException() {
        FleetVehicleEntity atual = persistReservation(DATE.minusDays(1), LocalTime.of(8, 0), "Almoxarifado", "Cliente X");

        assertThrows(SecurityException.class, () -> service.updateReservation(atual.getId(), validDto(), JWT));
        assertEquals("Almoxarifado", repository.findById(atual.getId()).orElseThrow().getDepartureAddress());
    }

    @Test
    void createReservation_persistsWithDefaultStatusAndType() {
        service.createReservation(validDto(), SOLICITANTE);

        List<FleetVehicleEntity> all = repository.findAll();
        assertEquals(1, all.size());

        FleetVehicleEntity saved = all.getFirst();
        assertNotNull(saved.getId());
        assertEquals(ReserveType.FLEET_VEHICLE, saved.getReservationType());
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, saved.getStatus());
        assertEquals(VehicleType.CARRO, saved.getTypeVehicle());
        assertEquals("TI", saved.getRequesterDepartment());
        assertEquals(3, saved.getQuantityPassengers());
    }

    @Test
    void createReservation_sameDepartureAndDestination_throws() {
        FleetVehicleRequestDTO dto = new FleetVehicleRequestDTO(
                DATE, LocalTime.of(8, 0), "Visita técnica", "Sede", "Sede",
                Boolean.FALSE, null,
                Boolean.TRUE, LocalTime.of(18, 0), "Sede", "Sede", "11999998888",
                "Fulano de Tal", "TI", Boolean.FALSE, null, VehicleType.CARRO, 3, null, null);

        assertThrows(SameDepartureAndDestinationException.class,
                () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_sameDateAsExistingReservation_isAllowed() {
        persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X");

        FleetVehicleRequestDTO dto = new FleetVehicleRequestDTO(
                DATE, LocalTime.of(9, 0), "Visita técnica", "Sede", "Cliente Y",
                Boolean.FALSE, null,
                Boolean.TRUE, LocalTime.of(18, 0), "Cliente Y", "Sede", "11999998888",
                "Fulano de Tal", "TI", Boolean.FALSE, null, VehicleType.CARRO, 3,
                user.getId(), null);

        service.createReservation(dto, SOLICITANTE);

        assertEquals(2, repository.count());
    }

    @Test
    void createReservation_lessThanTwoHoursAfterApprovedExit_throws() {
        persistWithStatus(ReserveStatus.APROVADA);

        FleetVehicleRequestDTO dto = new FleetVehicleRequestDTO(
                DATE, LocalTime.of(9, 0), "Visita técnica", "Sede", "Cliente Y",
                Boolean.FALSE, null,
                Boolean.TRUE, LocalTime.of(18, 0), "Cliente Y", "Sede", "11999998888",
                "Fulano de Tal", "TI", Boolean.FALSE, null, VehicleType.CARRO, 3,
                user.getId(), null);

        assertThrows(InvalidTimeBetweenReservations.class,
                () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(1, repository.count());
    }

    @Test
    void createReservation_twoHoursAfterApprovedExit_isAllowed() {
        persistWithStatus(ReserveStatus.APROVADA);

        FleetVehicleRequestDTO dto = new FleetVehicleRequestDTO(
                DATE, LocalTime.of(10, 0), "Visita técnica", "Sede", "Cliente Y",
                Boolean.FALSE, null,
                Boolean.TRUE, LocalTime.of(18, 0), "Cliente Y", "Sede", "11999998888",
                "Fulano de Tal", "TI", Boolean.FALSE, null, VehicleType.CARRO, 3,
                user.getId(), null);

        service.createReservation(dto, SOLICITANTE);

        assertEquals(2, repository.count());
    }

    @Test
    void createReservation_approvedExitOnAnotherDay_isAllowed() {
        FleetVehicleEntity outroDia = persistReservation(DATE.plusDays(1), LocalTime.of(8, 0), "Sede", "Cliente X");
        outroDia.setStatus(ReserveStatus.APROVADA);
        repository.saveAndFlush(outroDia);

        FleetVehicleRequestDTO dto = new FleetVehicleRequestDTO(
                DATE, LocalTime.of(9, 0), "Visita técnica", "Sede", "Cliente Y",
                Boolean.FALSE, null,
                Boolean.TRUE, LocalTime.of(18, 0), "Cliente Y", "Sede", "11999998888",
                "Fulano de Tal", "TI", Boolean.FALSE, null, VehicleType.CARRO, 3,
                user.getId(), null);

        service.createReservation(dto, SOLICITANTE);

        assertEquals(2, repository.count());
    }

    @Test
    void getReservationFromId_returnsDto() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();

        FleetVehicleResponseDTO dto = service.getReservationFromId(id);

        assertEquals(id, dto.id());
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
        persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X");

        List<FleetVehicleResponseDTO> all = service.getAllReservation();

        assertEquals(1, all.size());
    }

    @Test
    void approve_setsStatusApproved() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.approve(id, JWT);

        FleetVehicleEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.APROVADA, reloaded.getStatus());
        assertEquals("Aprovador Teste", reloaded.getUserNameApprover());
    }

    @Test
    void approve_alreadyApproved_throws() {
        Long id = persistWithStatus(ReserveStatus.APROVADA).getId();

        assertThrows(ReservationWasApprove.class, () -> service.approve(id, JWT));
    }

    @Test
    void reject_setsStatusRejectedWithReason() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.reject(id, Map.of("reasonFailure", "Frota em manutenção"), JWT);

        FleetVehicleEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.REPROVADA, reloaded.getStatus());
        assertEquals("Frota em manutenção", reloaded.getReasonFailure());
        assertEquals("Aprovador Teste", reloaded.getUserNameApprover());
    }

    @Test
    void reject_alreadyCancelled_throws() {
        Long id = persistWithStatus(ReserveStatus.CANCELADA).getId();

        assertThrows(ReservationWasCancell.class,
                () -> service.reject(id, Map.of("reasonFailure", "Frota em manutenção"), JWT));
    }

    @Test
    void updateReservation_updatesExisting() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();

        FleetVehicleRequestDTO update = new FleetVehicleRequestDTO(
                DATE.plusDays(1), LocalTime.of(7, 30), "Transporte de equipe", "Sede", "Evento",
                Boolean.FALSE, null,
                Boolean.FALSE, null, null, null, "11888887777", "Fulano", "RH",
                Boolean.TRUE, "Grupo grande", VehicleType.VAN, 12, null, null);

        service.updateReservation(id, update, SOLICITANTE);

        FleetVehicleEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(12, reloaded.getQuantityPassengers());
        assertEquals("RH", reloaded.getRequesterDepartment());
        assertEquals(VehicleType.VAN, reloaded.getTypeVehicle());
        assertTrue(reloaded.isNecessaryVan());
        assertEquals("Transporte de equipe", reloaded.getReason());
    }

    @Test
    void updateReservation_sameDate_throwsConflict() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();

        assertThrows(ReservationConflictException.class,
                () -> service.updateReservation(id, validDto(), SOLICITANTE));
    }

    @Test
    void updateReservation_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.updateReservation(999_999L, validDto(), SOLICITANTE));
    }

    @Test
    void createReservation_persistsUserAndTelephone() {
        service.createReservation(validDto(), SOLICITANTE);

        FleetVehicleEntity saved = repository.findAll().getFirst();
        assertNotNull(saved.getUser());
        assertEquals(user.getId(), saved.getUser().getId());
        assertEquals("1140028922", saved.getTelephone());
    }

    @Test
    void createReservation_unknownUserId_throws() {
        FleetVehicleRequestDTO dto = new FleetVehicleRequestDTO(
                DATE, LocalTime.of(8, 0), "Visita técnica", "Sede", "Cliente X",
                Boolean.FALSE, null,
                Boolean.TRUE, LocalTime.of(18, 0), "Cliente X", "Sede", "11999998888",
                "Fulano de Tal", "TI", Boolean.FALSE, null, VehicleType.CARRO, 3,
                999_999L, "1140028922");

        assertThrows(UserIdNotFoundedException.class, () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void createReservation_semUserId_lancaUserIdNotFounded() {
        FleetVehicleRequestDTO dto = new FleetVehicleRequestDTO(
                DATE, LocalTime.of(8, 0), "Visita técnica", "Sede", "Cliente X",
                Boolean.FALSE, null,
                Boolean.TRUE, LocalTime.of(18, 0), "Cliente X", "Sede", "11999998888",
                "Fulano de Tal", "TI", Boolean.FALSE, null, VehicleType.CARRO, 3,
                null, null);

        assertThrows(UserIdNotFoundedException.class, () -> service.createReservation(dto, SOLICITANTE));
        assertEquals(0, repository.count());
    }

    @Test
    void getReservationFromId_returnsNewFields() {
        FleetVehicleResponseDTO created = service.createReservation(validDto(), SOLICITANTE);

        FleetVehicleResponseDTO dto = service.getReservationFromId(created.id());

        assertEquals(user.getId(), dto.userId());
        assertEquals("1140028922", dto.telephone());
    }

    @Test
    void updateReservation_changesUserAndTelephone() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();
        UserEntity outro = persistOtherUser();

        FleetVehicleRequestDTO update = new FleetVehicleRequestDTO(
                DATE.plusDays(1), LocalTime.of(7, 30), "Transporte de equipe", "Sede", "Evento",
                Boolean.FALSE, null,
                Boolean.FALSE, null, null, null, "11888887777", "Fulano", "RH",
                Boolean.FALSE, null, VehicleType.CARRO, 3, outro.getId(), "1130001111");

        service.updateReservation(id, update, SOLICITANTE);

        FleetVehicleEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(outro.getId(), reloaded.getUser().getId());
        assertEquals("1130001111", reloaded.getTelephone());
    }

    @Test
    void updateReservation_withoutUserId_keepsCurrentUser() {
        Long id = service.createReservation(validDto(), SOLICITANTE).id();

        FleetVehicleRequestDTO update = new FleetVehicleRequestDTO(
                DATE.plusDays(1), LocalTime.of(7, 30), "Transporte de equipe", "Sede", "Evento",
                Boolean.FALSE, null,
                Boolean.FALSE, null, null, null, "11888887777", "Fulano", "RH",
                Boolean.FALSE, null, VehicleType.CARRO, 3, null, "1130001111");

        service.updateReservation(id, update, SOLICITANTE);

        FleetVehicleEntity reloaded = repository.findById(id).orElseThrow();
        assertNotNull(reloaded.getUser());
        assertEquals(user.getId(), reloaded.getUser().getId());
    }

    @Test
    void deleteReserve_removesExisting() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();

        service.deleteReserve(id);

        assertFalse(repository.existsById(id));
    }

    @Test
    void deleteReserve_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.deleteReserve(999_999L));
    }

    @Test
    void getReservationFromYearAndMonth_trazSoOMesEAnoPedidos() {
        Long doMes = persistReservation(LocalDate.of(2026, 8, 10), LocalTime.of(8, 0), "Sede", "Cliente X").getId();
        persistReservation(LocalDate.of(2026, 9, 10), LocalTime.of(8, 0), "Sede", "Cliente X");
        persistReservation(LocalDate.of(2025, 8, 10), LocalTime.of(8, 0), "Sede", "Cliente X");

        List<FleetVehicleResponseDTO> encontradas = service.getReservationFromYearAndMonth(2026, 8);

        assertEquals(1, encontradas.size());
        assertEquals(doMes, encontradas.get(0).id());
    }

    @Test
    void getReservationFromYearAndMonth_semReservaNoMes_retornaVazio() {
        persistReservation(LocalDate.of(2026, 8, 10), LocalTime.of(8, 0), "Sede", "Cliente X");

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

    private FleetVehicleEntity persistWithUserAndStatus(UserEntity dono, ReserveStatus status) {
        FleetVehicleEntity e = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X");
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

        FleetVehicleEntity cancelada = repository.findById(id).orElseThrow();
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

        FleetVehicleEntity cancelada = repository.findById(id).orElseThrow();
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

        verify(smtpService).sendEmail(any(FleetVehicleEntity.class));
    }

    @Test
    void createReservation_avisaOSolicitantePorEmail() {
        service.createReservation(validDto(), SOLICITANTE);

        verify(smtpService).sendEmail(any(FleetVehicleEntity.class));
    }

    @Test
    void approve_avisaOSolicitantePorEmail() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.approve(id, JWT);

        verify(smtpService).sendEmail(any(FleetVehicleEntity.class));
    }

    @Test
    void reject_avisaOSolicitantePorEmail() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();
        given(cognitoUserServiceImpl.getName(JWT.getTokenValue())).willReturn("Aprovador Teste");

        service.reject(id, Map.of("reasonFailure", "Frota em manutenção"), JWT);

        verify(smtpService).sendEmail(any(FleetVehicleEntity.class));
    }
}
