package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.dto.reservation.auditorium.AuditoriumRequestDTO;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.exception.InvalidTimeException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.ReservationWasCancell;
import com.siurbinfo.srs.repository.reservation.ReservationAuditoriumRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuditoriumValidatorTest {

    private static final Long ID_RESERVA = 1L;
    private static final String EMAIL = "solicitante@teste.com";
    private static final Long ID_DONO = 10L;
    private static final Long ID_OUTRO = 99L;

    private static final List<String> SEM_PRIVILEGIO = List.of("siurb-usuario");
    private static final List<String> ADMINISTRADOR = List.of("siurb-administrador");

    private ReservationAuditoriumRepository repository;
    private UserRepository userRepository;
    private AuditoriumValidator validator;

    @BeforeEach
    void setUp() {
        repository = mock(ReservationAuditoriumRepository.class);
        userRepository = mock(UserRepository.class);
        validator = new AuditoriumValidator(new ReservationValidator<>(), repository, userRepository);
    }

    private AuditoriumEntity reserva(ReserveStatus status, Long idDono) {
        AuditoriumEntity entity = new AuditoriumEntity();
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

    @Test
    void validatorCancel_emailSemUsuarioNoBanco_estouraNullPointer() {
        when(repository.findById(ID_RESERVA))
                .thenReturn(Optional.of(reserva(ReserveStatus.ENVIADA_PARA_ANALISE, ID_DONO)));
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> validator.validatorCancel(ID_RESERVA, EMAIL, SEM_PRIVILEGIO));
    }

    private static AuditoriumRequestDTO dto(Boolean necessaryCoffee, LocalTime montagem, LocalTime servico) {
        return new AuditoriumRequestDTO(
                LocalDate.of(2026, 8, 1), LocalTime.of(8, 30), "Palestra", null, LocalTime.of(9, 0),
                LocalTime.of(12, 0), "Fulano", "TI", 50, Boolean.FALSE,
                Boolean.TRUE, Boolean.TRUE, necessaryCoffee, montagem, servico, null,
                ID_DONO, null, null, null, null);
    }

    @Test
    void validatorCreate_semCoffeeEHorariosNulos_naoLanca() {
        assertDoesNotThrow(() -> validator.validatorCreate(dto(false, null, null), SEM_PRIVILEGIO));
    }

    @Test
    void validatorCreate_semCoffee_ignoraOsHorarios() {
        assertDoesNotThrow(() -> validator.validatorCreate(
                dto(false, LocalTime.of(13, 0), LocalTime.of(8, 0)), SEM_PRIVILEGIO));
    }

    @Test
    void validatorCreate_comCoffeeSemHorarios_naoLanca() {
        assertDoesNotThrow(() -> validator.validatorCreate(dto(true, null, null), SEM_PRIVILEGIO));
    }

    @Test
    void validatorCreate_comCoffeeEHorariosFolgados_naoLanca() {
        assertDoesNotThrow(() -> validator.validatorCreate(
                dto(true, LocalTime.of(10, 0), LocalTime.of(10, 15)), SEM_PRIVILEGIO));
    }

    @Test
    void validatorCreate_montagemDepoisDoServico_lancaInvalidTime() {
        InvalidTimeException ex = assertThrows(InvalidTimeException.class, () -> validator.validatorCreate(
                dto(true, LocalTime.of(10, 30), LocalTime.of(10, 0)), SEM_PRIVILEGIO));

        assertEquals("O Horário de montagem tem que ser antes do horário para servir", ex.getMessage());
    }

    @Test
    void validatorCreate_montagemAntesDaAbertura_lancaInvalidTime() {
        InvalidTimeException ex = assertThrows(InvalidTimeException.class, () -> validator.validatorCreate(
                dto(true, LocalTime.of(8, 0), LocalTime.of(10, 0)), SEM_PRIVILEGIO));

        assertEquals("O horário para a montagem precisa estar dentro do horário da reserva", ex.getMessage());
    }

    @Test
    void validatorCreate_servicoDepoisDoFim_lancaInvalidTime() {
        InvalidTimeException ex = assertThrows(InvalidTimeException.class, () -> validator.validatorCreate(
                dto(true, LocalTime.of(10, 0), LocalTime.of(12, 30)), SEM_PRIVILEGIO));

        assertEquals("O horário para a servir precisa estar dentro do horário da reserva", ex.getMessage());
    }

    @Test
    void validatorCreate_montagemAMenosDe30MinDoFim_lancaInvalidTime() {
        InvalidTimeException ex = assertThrows(InvalidTimeException.class, () -> validator.validatorCreate(
                dto(true, LocalTime.of(11, 40), LocalTime.of(11, 50)), SEM_PRIVILEGIO));

        assertEquals("É necessário que a montagem seja no mínimo 30 minutos antes do fim da reserva", ex.getMessage());
    }

    @Test
    void validatorCreate_montagemExatamente30MinAntesDoFim_naoLanca() {
        assertDoesNotThrow(() -> validator.validatorCreate(
                dto(true, LocalTime.of(11, 30), LocalTime.of(11, 40)), SEM_PRIVILEGIO));
    }

    @Test
    void validatorCreate_menosDe10MinPraMontar_lancaInvalidTime() {
        InvalidTimeException ex = assertThrows(InvalidTimeException.class, () -> validator.validatorCreate(
                dto(true, LocalTime.of(10, 0), LocalTime.of(10, 5)), SEM_PRIVILEGIO));

        assertEquals("É necessário pelo menos 10 minutos de para a montagem da mesa", ex.getMessage());
    }

    @Test
    void validatorCreate_montagemEServicoNoMesmoHorario_lancaInvalidTime() {
        assertThrows(InvalidTimeException.class, () -> validator.validatorCreate(
                dto(true, LocalTime.of(10, 0), LocalTime.of(10, 0)), SEM_PRIVILEGIO));
    }

    @Test
    void validatorCreate_soMontagemInformada_validaSoAMontagem() {
        assertDoesNotThrow(() -> validator.validatorCreate(
                dto(true, LocalTime.of(10, 0), null), SEM_PRIVILEGIO));
        assertThrows(InvalidTimeException.class, () -> validator.validatorCreate(
                dto(true, LocalTime.of(11, 45), null), SEM_PRIVILEGIO));
    }

    @Test
    void validatorCreate_soServicoInformado_validaSoOServico() {
        assertDoesNotThrow(() -> validator.validatorCreate(
                dto(true, null, LocalTime.of(10, 0)), SEM_PRIVILEGIO));
        assertThrows(InvalidTimeException.class, () -> validator.validatorCreate(
                dto(true, null, LocalTime.of(12, 30)), SEM_PRIVILEGIO));
    }

    @Test
    void validatorCreate_usuarioEspecial_aindaValidaCoffee() {
        assertThrows(InvalidTimeException.class, () -> validator.validatorCreate(
                dto(true, LocalTime.of(10, 30), LocalTime.of(10, 0)), List.of("siurb-usuario-geral-especial")));
    }

    @Test
    void validatorUpdate_semCoffeeEHorariosNulos_naoLanca() {
        assertDoesNotThrow(() -> validator.validatorUpdate(
                dto(false, null, null), reserva(ReserveStatus.ENVIADA_PARA_ANALISE, ID_DONO)));
    }

    @Test
    void validatorUpdate_montagemDepoisDoServico_lancaInvalidTime() {
        assertThrows(InvalidTimeException.class, () -> validator.validatorUpdate(
                dto(true, LocalTime.of(10, 30), LocalTime.of(10, 0)), reserva(ReserveStatus.ENVIADA_PARA_ANALISE, ID_DONO)));
    }
}
