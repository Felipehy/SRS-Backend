package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.config.TimeZoneConfig;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleRequestDTO;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.VehicleType;
import com.siurbinfo.srs.exception.InvalidReasonException;
import com.siurbinfo.srs.exception.InvalidReservationException;
import com.siurbinfo.srs.exception.InvalidTimeBetweenReservations;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.ReservationWasCancell;
import com.siurbinfo.srs.repository.reservation.ReservationFleetVehicleRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VehicleValidatorTest {

    private static final Long ID_RESERVA = 1L;
    private static final String EMAIL = "solicitante@teste.com";
    private static final Long ID_DONO = 10L;
    private static final Long ID_OUTRO = 99L;

    private static final List<String> SEM_PRIVILEGIO = List.of("siurb-usuario");
    private static final List<String> ADMINISTRADOR = List.of("siurb-administrador");

    private ReservationFleetVehicleRepository repository;
    private UserRepository userRepository;
    private VehicleValidator validator;

    @BeforeEach
    void setUp() {
        repository = mock(ReservationFleetVehicleRepository.class);
        userRepository = mock(UserRepository.class);
        validator = new VehicleValidator(new ReservationValidator<>(), repository, userRepository);
    }

    private FleetVehicleEntity reserva(ReserveStatus status, Long idDono) {
        FleetVehicleEntity entity = new FleetVehicleEntity();
        entity.setId(ID_RESERVA);
        entity.setStatus(status);
        if (idDono != null) {
            UserEntity dono = new UserEntity();
            dono.setId(idDono);
            entity.setUser(dono);
        }
        return entity;
    }

    @Test
    void validatorCancel_reservaInexistente_lancaReservationIdIsNotFounded() {
        when(repository.findById(ID_RESERVA)).thenReturn(Optional.empty());

        ReservationIdIsNotFoundedException ex = assertThrows(ReservationIdIsNotFoundedException.class,
                () -> validator.validatorCancel(ID_RESERVA, EMAIL, SEM_PRIVILEGIO));

        assertEquals("Não foi encontrado a reserva", ex.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    void validatorCancel_donoDaReserva_naoLanca() {
        when(repository.findById(ID_RESERVA))
                .thenReturn(Optional.of(reserva(ReserveStatus.ENVIADA_PARA_ANALISE, ID_DONO)));
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_DONO);

        assertDoesNotThrow(() -> validator.validatorCancel(ID_RESERVA, EMAIL, SEM_PRIVILEGIO));
    }

    @Test
    void validatorCancel_outroUsuarioSemPrivilegio_lancaSecurityException() {
        when(repository.findById(ID_RESERVA))
                .thenReturn(Optional.of(reserva(ReserveStatus.ENVIADA_PARA_ANALISE, ID_DONO)));
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_OUTRO);

        assertThrows(SecurityException.class,
                () -> validator.validatorCancel(ID_RESERVA, EMAIL, SEM_PRIVILEGIO));
    }

    @Test
    void validatorCancel_outroUsuarioAdministrador_naoLanca() {
        when(repository.findById(ID_RESERVA))
                .thenReturn(Optional.of(reserva(ReserveStatus.ENVIADA_PARA_ANALISE, ID_DONO)));
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_OUTRO);

        assertDoesNotThrow(() -> validator.validatorCancel(ID_RESERVA, EMAIL, ADMINISTRADOR));
    }

    @Test
    void validatorCancel_reservaJaCancelada_lancaReservationWasCancell() {
        when(repository.findById(ID_RESERVA))
                .thenReturn(Optional.of(reserva(ReserveStatus.CANCELADA, ID_DONO)));
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_DONO);

        assertThrows(ReservationWasCancell.class,
                () -> validator.validatorCancel(ID_RESERVA, EMAIL, SEM_PRIVILEGIO));
    }

    @Test
    void validatorCancel_reservaSemUsuarioVinculado_estouraNullPointer() {
        when(repository.findById(ID_RESERVA))
                .thenReturn(Optional.of(reserva(ReserveStatus.ENVIADA_PARA_ANALISE, null)));
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_DONO);

        assertThrows(NullPointerException.class,
                () -> validator.validatorCancel(ID_RESERVA, EMAIL, SEM_PRIVILEGIO));
    }

    private static LocalDate hoje() {
        return LocalDate.now(TimeZoneConfig.ZONE);
    }

    private static LocalDate amanha() {
        return hoje().plusDays(1);
    }

    private FleetVehicleRequestDTO dto(LocalDate data, LocalTime saida, boolean van, String motivoVan, int passageiros) {
        return new FleetVehicleRequestDTO(
                data, saida, "Visita técnica", "Sede", "Cliente X",
                Boolean.FALSE, null,
                Boolean.FALSE, null, null, null, "11999998888",
                "Fulano de Tal", "TI", van, motivoVan,
                van ? VehicleType.VAN : VehicleType.CARRO, passageiros,
                ID_DONO, null);
    }

    private FleetVehicleRequestDTO dto(LocalDate data, LocalTime saida) {
        return dto(data, saida, false, null, 3);
    }

    @Test
    void validateCreate_vanComQuatroPassageiros_lancaInvalidReservation() {
        InvalidReservationException ex = assertThrows(InvalidReservationException.class,
                () -> validator.validateCreate(dto(amanha(), LocalTime.of(9, 0), true, "Equipe de campo", 4), SEM_PRIVILEGIO));

        assertEquals("Quantidade mínima para van não atingida (5 pessoas)", ex.getMessage());
    }

    @Test
    void validateCreate_vanComCincoPassageiros_naoLanca() {
        assertDoesNotThrow(
                () -> validator.validateCreate(dto(amanha(), LocalTime.of(9, 0), true, "Equipe de campo", 5), SEM_PRIVILEGIO));
    }

    @Test
    void validateCreate_semVanComUmPassageiro_naoLanca() {
        assertDoesNotThrow(
                () -> validator.validateCreate(dto(amanha(), LocalTime.of(9, 0), false, null, 1), SEM_PRIVILEGIO));
    }

    @Test
    void validateCreate_vanSemMotivo_lancaInvalidReason() {
        InvalidReasonException ex = assertThrows(InvalidReasonException.class,
                () -> validator.validateCreate(dto(amanha(), LocalTime.of(9, 0), true, null, 8), SEM_PRIVILEGIO));

        assertEquals("É necessário colocar um motivo para pedir a van", ex.getMessage());
    }

    @Test
    void validateCreate_vanComMotivoEmBranco_lancaInvalidReason() {
        assertThrows(InvalidReasonException.class,
                () -> validator.validateCreate(dto(amanha(), LocalTime.of(9, 0), true, "   ", 8), SEM_PRIVILEGIO));
    }

    @Test
    void validateCreate_saidaUmaHoraDepoisDeOutraAprovada_lancaInvalidTimeBetweenReservations() {
        LocalDate data = amanha();
        when(repository.findByReservationDateAndStartTime(data, LocalTime.of(9, 0), "APROVADA"))
                .thenReturn(Optional.of(LocalTime.of(8, 0)));

        InvalidTimeBetweenReservations ex = assertThrows(InvalidTimeBetweenReservations.class,
                () -> validator.validateCreate(dto(data, LocalTime.of(9, 0)), SEM_PRIVILEGIO));

        assertEquals("O tempo entre as reservas tem que ser de 2 horas", ex.getMessage());
    }

    @Test
    void validateCreate_saidaExatamenteDuasHorasDepois_naoLanca() {
        LocalDate data = amanha();
        when(repository.findByReservationDateAndStartTime(data, LocalTime.of(10, 0), "APROVADA"))
                .thenReturn(Optional.of(LocalTime.of(8, 0)));

        assertDoesNotThrow(() -> validator.validateCreate(dto(data, LocalTime.of(10, 0)), SEM_PRIVILEGIO));
    }

    @Test
    void validateCreate_consultaSaidaAnteriorComStatusAprovadaComoTexto() {
        LocalDate data = amanha();

        validator.validateCreate(dto(data, LocalTime.of(9, 0)), SEM_PRIVILEGIO);

        verify(repository).findByReservationDateAndStartTime(data, LocalTime.of(9, 0), "APROVADA");
    }

    @Test
    void validateCreate_tresDiasDeAntecedencia_lancaInvalidReservation() {
        InvalidReservationException ex = assertThrows(InvalidReservationException.class,
                () -> validator.validateCreate(dto(hoje().plusDays(3), LocalTime.of(9, 0)), SEM_PRIVILEGIO));

        assertEquals("Tem que ter no máximo 2 dias de antecipação", ex.getMessage());
    }

    @Test
    void validateCreate_exatamenteDoisDiasDeAntecedencia_naoLanca() {
        assertDoesNotThrow(
                () -> validator.validateCreate(dto(hoje().plusDays(2), LocalTime.of(9, 0)), SEM_PRIVILEGIO));
    }

    @Test
    void validateCreate_paraHoje_naoLanca() {
        assertDoesNotThrow(() -> validator.validateCreate(dto(hoje(), LocalTime.of(9, 0)), SEM_PRIVILEGIO));
    }

    private static final List<String> ESPECIAL = List.of("siurb-usuario-geral-especial");

    @Test
    void validateCreate_usuarioEspecial_ignoraIntervaloDeDuasHoras() {
        LocalDate data = amanha();
        when(repository.findByReservationDateAndStartTime(data, LocalTime.of(9, 0), "APROVADA"))
                .thenReturn(Optional.of(LocalTime.of(8, 0)));

        assertDoesNotThrow(() -> validator.validateCreate(dto(data, LocalTime.of(9, 0)), ESPECIAL));
    }

    @Test
    void validateCreate_usuarioEspecial_ignoraLimiteDeAntecedencia() {
        assertDoesNotThrow(
                () -> validator.validateCreate(dto(hoje().plusDays(10), LocalTime.of(9, 0)), ESPECIAL));
    }

    @Test
    void validateCreate_usuarioEspecial_aindaExigeMinimoDaVan() {
        assertThrows(InvalidReservationException.class,
                () -> validator.validateCreate(dto(amanha(), LocalTime.of(9, 0), true, "Equipe de campo", 4), ESPECIAL));
    }

    @Test
    void validateCreate_grupoEspecialSemDiferenciarMaiusculas() {
        assertDoesNotThrow(() -> validator.validateCreate(
                dto(hoje().plusDays(10), LocalTime.of(9, 0)), List.of("SIURB-USUARIO-GERAL-ESPECIAL")));
    }

    @Test
    void validateUpdate_usuarioEspecial_aindaBarraMesmaData() {
        FleetVehicleEntity atual = reservaEm(amanha());

        assertThrows(ReservationConflictException.class,
                () -> validator.validateUpdate(dto(amanha(), LocalTime.of(9, 0)), atual, ESPECIAL));
    }

    private FleetVehicleEntity reservaEm(LocalDate data) {
        FleetVehicleEntity entity = reserva(ReserveStatus.ENVIADA_PARA_ANALISE, ID_DONO);
        entity.setReservationDate(data);
        return entity;
    }

    @Test
    void validateUpdate_tresDiasDeAntecedencia_lancaInvalidReservation() {
        FleetVehicleEntity atual = reservaEm(hoje());

        assertThrows(InvalidReservationException.class,
                () -> validator.validateUpdate(dto(hoje().plusDays(3), LocalTime.of(9, 0)), atual, SEM_PRIVILEGIO));
    }

    @Test
    void validateUpdate_saidaUmaHoraDepoisDeOutraAprovada_lancaInvalidTimeBetweenReservations() {
        LocalDate novaData = amanha();
        FleetVehicleEntity atual = reservaEm(hoje());
        when(repository.findByReservationDateAndStartTime(novaData, LocalTime.of(9, 0), "APROVADA"))
                .thenReturn(Optional.of(LocalTime.of(8, 0)));

        assertThrows(InvalidTimeBetweenReservations.class,
                () -> validator.validateUpdate(dto(novaData, LocalTime.of(9, 0)), atual, SEM_PRIVILEGIO));
    }

    @Test
    void validateUpdate_dataValidaSemSaidaAprovadaAntes_naoLanca() {
        FleetVehicleEntity atual = reservaEm(hoje());

        assertDoesNotThrow(() -> validator.validateUpdate(dto(amanha(), LocalTime.of(9, 0)), atual, SEM_PRIVILEGIO));
    }

    @Test
    void validateUpdate_vanComQuatroPassageiros_lancaInvalidReservation() {
        FleetVehicleEntity atual = reservaEm(hoje());

        assertThrows(InvalidReservationException.class,
                () -> validator.validateUpdate(dto(amanha(), LocalTime.of(9, 0), true, "Equipe de campo", 4), atual, SEM_PRIVILEGIO));
    }

    @Test
    void validateUpdate_vanSemMotivo_lancaInvalidReason() {
        FleetVehicleEntity atual = reservaEm(hoje());

        assertThrows(InvalidReasonException.class,
                () -> validator.validateUpdate(dto(amanha(), LocalTime.of(9, 0), true, null, 8), atual, SEM_PRIVILEGIO));
    }

    @Test
    void validateUpdate_foraDoMunicipioSemMotivo_lancaInvalidReason() {
        FleetVehicleEntity atual = reservaEm(hoje());
        FleetVehicleRequestDTO foraSemMotivo = new FleetVehicleRequestDTO(
                amanha(), LocalTime.of(9, 0), "Visita técnica", "Sede", "Guarulhos",
                Boolean.TRUE, null,
                Boolean.FALSE, null, null, null, "11999998888",
                "Fulano de Tal", "TI", false, null, VehicleType.CARRO, 3,
                ID_DONO, null);

        InvalidReasonException ex = assertThrows(InvalidReasonException.class,
                () -> validator.validateUpdate(foraSemMotivo, atual, SEM_PRIVILEGIO));

        assertEquals("É necessário colocar o motivo do endereço fora do município", ex.getMessage());
    }

    @Test
    void validateUpdate_mesmaData_lancaConflitoAntesDasOutrasRegras() {
        FleetVehicleEntity atual = reservaEm(amanha());

        assertThrows(ReservationConflictException.class,
                () -> validator.validateUpdate(dto(amanha(), LocalTime.of(9, 0), true, null, 2), atual, SEM_PRIVILEGIO));
    }
}
