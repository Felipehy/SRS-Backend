package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.config.TimeZoneConfig;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.exception.InvalidReservationException;
import com.siurbinfo.srs.exception.InvalidTimeBetweenReservations;
import com.siurbinfo.srs.exception.InvalidTimeException;
import com.siurbinfo.srs.exception.ReservationWasCancell;
import com.siurbinfo.srs.repository.reservation.IreservationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservationValidatorTest {

    private final ReservationValidator<Object> validator = new ReservationValidator<>();

    private static final Long DONO = 10L;
    private static final Long OUTRO = 99L;

    private static final List<String> SEM_PRIVILEGIO = List.of("siurb-usuario");
    private static final List<String> ADMINISTRADOR = List.of("siurb-usuario", "siurb-administrador");
    private static final List<String> DIVISAO_ADMINISTRATIVA = List.of("siurb-divisao-administrativa");

    @Test
    void isPossibleCanceled_reservaJaCancelada_lancaReservationWasCancell() {
        ReservationWasCancell ex = assertThrows(ReservationWasCancell.class,
                () -> validator.isPossibleCanceled(ReserveStatus.CANCELADA, DONO, DONO, SEM_PRIVILEGIO));

        assertEquals("A reserva já foi cancelada", ex.getMessage());
    }

    @Test
    void isPossibleCanceled_reservaJaCanceladaEUsuarioAdministrador_aindaAssimLanca() {
        assertThrows(ReservationWasCancell.class,
                () -> validator.isPossibleCanceled(ReserveStatus.CANCELADA, OUTRO, DONO, ADMINISTRADOR));
    }

    @Test
    void isPossibleCanceled_donoDaReserva_naoLanca() {
        assertDoesNotThrow(
                () -> validator.isPossibleCanceled(ReserveStatus.ENVIADA_PARA_ANALISE, DONO, DONO, SEM_PRIVILEGIO));
    }

    @Test
    void isPossibleCanceled_donoDaReservaSemNenhumGrupo_naoLanca() {
        assertDoesNotThrow(
                () -> validator.isPossibleCanceled(ReserveStatus.ENVIADA_PARA_ANALISE, DONO, DONO, List.of()));
    }

    @Test
    void isPossibleCanceled_donoDeReservaAprovadaOuFinalizada_naoLanca() {
        assertDoesNotThrow(() -> validator.isPossibleCanceled(ReserveStatus.APROVADA, DONO, DONO, SEM_PRIVILEGIO));
        assertDoesNotThrow(() -> validator.isPossibleCanceled(ReserveStatus.REPROVADA, DONO, DONO, SEM_PRIVILEGIO));
        assertDoesNotThrow(() -> validator.isPossibleCanceled(ReserveStatus.FINALIZADA, DONO, DONO, SEM_PRIVILEGIO));
    }

    @Test
    void isPossibleCanceled_outroUsuarioSemPrivilegio_lancaSecurityException() {
        SecurityException ex = assertThrows(SecurityException.class,
                () -> validator.isPossibleCanceled(ReserveStatus.ENVIADA_PARA_ANALISE, OUTRO, DONO, SEM_PRIVILEGIO));

        assertEquals("Você não é o solicitante dessa reserva", ex.getMessage());
    }

    @Test
    void isPossibleCanceled_outroUsuarioSemNenhumGrupo_lancaSecurityException() {
        assertThrows(SecurityException.class,
                () -> validator.isPossibleCanceled(ReserveStatus.ENVIADA_PARA_ANALISE, OUTRO, DONO, List.of()));
    }

    @Test
    void isPossibleCanceled_outroUsuarioAdministrador_naoLanca() {
        assertDoesNotThrow(
                () -> validator.isPossibleCanceled(ReserveStatus.ENVIADA_PARA_ANALISE, OUTRO, DONO, ADMINISTRADOR));
    }

    @Test
    void isPossibleCanceled_outroUsuarioDivisaoAdministrativa_naoLanca() {
        assertDoesNotThrow(() -> validator.isPossibleCanceled(
                ReserveStatus.ENVIADA_PARA_ANALISE, OUTRO, DONO, DIVISAO_ADMINISTRATIVA));
    }

    @Test
    void isPossibleCanceled_grupoComOutraCaixa_naoLanca() {
        assertDoesNotThrow(() -> validator.isPossibleCanceled(
                ReserveStatus.ENVIADA_PARA_ANALISE, OUTRO, DONO, List.of("SIURB-Administrador")));
    }

    @Test
    void isPossibleCanceled_outroUsuarioComGruposNulos_lancaSecurityException() {
        SecurityException ex = assertThrows(SecurityException.class,
                () -> validator.isPossibleCanceled(ReserveStatus.ENVIADA_PARA_ANALISE, OUTRO, DONO, null));

        assertEquals("the user must be in a group", ex.getMessage());
    }

    @Test
    void isPossibleCanceled_donoComGruposNulos_lancaSecurityException() {
        assertThrows(SecurityException.class,
                () -> validator.isPossibleCanceled(ReserveStatus.ENVIADA_PARA_ANALISE, DONO, DONO, null));
    }

    @Test
    void isPossibleCanceled_reservaJaCanceladaComGruposNulos_lancaSecurityExceptionENaoReservationWasCancell() {
        assertThrows(SecurityException.class,
                () -> validator.isPossibleCanceled(ReserveStatus.CANCELADA, DONO, DONO, null));
    }

    @Test
    void isAfterSevenAM_antesDasSete_lancaInvalidTimeException() {
        InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                () -> validator.isAfterSevenAM(LocalTime.of(6, 59)));

        assertEquals("O horário precisa ser depois das 7 da manhã", ex.getMessage());
    }

    @Test
    void isAfterSevenAM_exatamenteSeteHoras_naoLanca() {
        assertDoesNotThrow(() -> validator.isAfterSevenAM(LocalTime.of(7, 0)));
    }

    @Test
    void isAfterSevenAM_depoisDasSete_naoLanca() {
        assertDoesNotThrow(() -> validator.isAfterSevenAM(LocalTime.of(9, 0)));
    }

    @Test
    void isAfterSevenAM_madrugada_lancaInvalidTimeException() {
        assertThrows(InvalidTimeException.class,
                () -> validator.isAfterSevenAM(LocalTime.of(0, 0)));
    }

    @Test
    void isOutsideBusinessHours_depoisDas19SemMotivo_lancaInvalidTimeException() {
        InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                () -> validator.isOutsideBusinessHours(LocalTime.of(19, 30), null));

        assertEquals("É necessário o motivo para reservas depois das 19h", ex.getMessage());
    }

    @Test
    void isOutsideBusinessHours_depoisDas19ComMotivoEmBranco_lanca() {
        assertThrows(InvalidTimeException.class,
                () -> validator.isOutsideBusinessHours(LocalTime.of(19, 30), "   "));
    }

    @Test
    void isOutsideBusinessHours_depoisDas19ComMotivoVazio_lanca() {
        assertThrows(InvalidTimeException.class,
                () -> validator.isOutsideBusinessHours(LocalTime.of(19, 30), ""));
    }

    @Test
    void isOutsideBusinessHours_depoisDas19ComMotivo_naoLanca() {
        assertDoesNotThrow(() -> validator.isOutsideBusinessHours(LocalTime.of(19, 30), "Evento noturno"));
    }

    @Test
    void isOutsideBusinessHours_exatamente19h_naoLanca() {
        assertDoesNotThrow(() -> validator.isOutsideBusinessHours(LocalTime.of(19, 0), null));
    }

    @Test
    void isOutsideBusinessHours_antesDas19SemMotivo_naoLanca() {
        assertDoesNotThrow(() -> validator.isOutsideBusinessHours(LocalTime.of(18, 0), null));
    }

    @Test
    void isOutsideBusinessHours_umMinutoDepoisDas19SemMotivo_lanca() {
        assertThrows(InvalidTimeException.class,
                () -> validator.isOutsideBusinessHours(LocalTime.of(19, 1), null));
    }

    @Test
    void validateIntervalDays_umDiaAlemDoLimite_lancaInvalidReservation() {
        InvalidReservationException ex = assertThrows(InvalidReservationException.class,
                () -> validator.validateIntervalDays(LocalDate.now(TimeZoneConfig.ZONE).plusDays(3), 2));

        assertEquals("Tem que ter no máximo 2 dias de antecipação", ex.getMessage());
    }

    @Test
    void validateIntervalDays_mensagemUsaOLimitePassado() {
        InvalidReservationException ex = assertThrows(InvalidReservationException.class,
                () -> validator.validateIntervalDays(LocalDate.now(TimeZoneConfig.ZONE).plusDays(6), 5));

        assertEquals("Tem que ter no máximo 5 dias de antecipação", ex.getMessage());
    }

    @Test
    void validateIntervalDays_noLimite_naoLanca() {
        assertDoesNotThrow(() -> validator.validateIntervalDays(LocalDate.now(TimeZoneConfig.ZONE).plusDays(2), 2));
    }

    @Test
    void validateIntervalDays_hoje_naoLanca() {
        assertDoesNotThrow(() -> validator.validateIntervalDays(LocalDate.now(TimeZoneConfig.ZONE), 2));
    }

    @Test
    void validateIntervalDays_dataPassada_naoLanca() {
        assertDoesNotThrow(() -> validator.validateIntervalDays(LocalDate.now(TimeZoneConfig.ZONE).minusDays(10), 2));
    }

    private static final LocalDate DIA = LocalDate.of(2026, 8, 1);

    @SuppressWarnings("unchecked")
    private final IreservationRepository<Object> repository = mock(IreservationRepository.class);

    @Test
    void isStartiTimeStartAfterATime_intervaloEmHoras_mensagemEmHoras() {
        when(repository.findByReservationDateAndStartTime(DIA, LocalTime.of(9, 0), "APROVADA"))
                .thenReturn(Optional.of(LocalTime.of(8, 0)));

        InvalidTimeBetweenReservations ex = assertThrows(InvalidTimeBetweenReservations.class,
                () -> validator.isStartiTimeStartAfterATime(DIA, LocalTime.of(9, 0), 120, repository));

        assertEquals("O tempo entre as reservas tem que ser de 2 horas", ex.getMessage());
    }

    @Test
    void isStartiTimeStartAfterATime_intervaloEmMinutos_mensagemEmMinutos() {
        when(repository.findByReservationDateAndStartTime(DIA, LocalTime.of(10, 10), "APROVADA"))
                .thenReturn(Optional.of(LocalTime.of(10, 0)));

        InvalidTimeBetweenReservations ex = assertThrows(InvalidTimeBetweenReservations.class,
                () -> validator.isStartiTimeStartAfterATime(DIA, LocalTime.of(10, 10), 15, repository));

        assertEquals("O tempo entre as reservas tem que ser de 15 minutos", ex.getMessage());
    }

    @Test
    void isStartiTimeStartAfterATime_semReservaAprovadaAntes_naoLanca() {
        when(repository.findByReservationDateAndStartTime(DIA, LocalTime.of(9, 0), "APROVADA"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.isStartiTimeStartAfterATime(DIA, LocalTime.of(9, 0), 120, repository));
    }
}
