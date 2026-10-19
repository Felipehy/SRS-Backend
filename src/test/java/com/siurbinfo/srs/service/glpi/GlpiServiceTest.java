package com.siurbinfo.srs.service.glpi;

import com.siurbinfo.srs.dto.glpi.GlpiRequestDTO;
import com.siurbinfo.srs.dto.glpi.GlpiResponseDTO;
import com.siurbinfo.srs.dto.glpi.GlpiSessionTokenDTO;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.repository.reservation.ReservationAuditoriumRepository;
import com.siurbinfo.srs.service.smtp.SmtpService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GlpiServiceTest {

    private static final String FORMATO_GLPI = "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}";

    private ReservationAuditoriumRepository repository;
    private SmtpService smtpService;
    private GlpiService service;

    @BeforeEach
    void setUp() {
        repository = mock(ReservationAuditoriumRepository.class);
        smtpService = mock(SmtpService.class);
        service = new GlpiService(repository, smtpService);
    }

    private AuditoriumEntity auditorium() {
        AuditoriumEntity e = new AuditoriumEntity();
        e.setId(1L);
        e.setReservationDate(LocalDate.of(2026, 8, 31));
        e.setTimeOpenAuditorium(LocalTime.of(8, 30));
        e.setStartTime(LocalTime.of(9, 0));
        e.setEndTime(LocalTime.of(11, 0));
        e.setReason("Palestra de teste");
        e.setUserNameRequester("Fulano de Tal");
        e.setNumPeople(50);
        e.setExternalPublic(true);
        e.setNumExternalPeople(10);
        e.setTelephone("1140028922");
        e.setNecessaryScreen(true);
        e.setNecessarySoundSystem(true);

        UserEntity user = new UserEntity();
        user.setEmail("solicitante@teste.com");
        e.setUser(user);

        return e;
    }

    private GlpiRequestDTO ticketDe(AuditoriumEntity entity) {
        return service.createBody(entity).get("input");
    }

    @Test
    void createBody_resolutionDateSaiNoFormatoDoGlpi() {
        GlpiRequestDTO ticket = ticketDe(auditorium());

        assertEquals("2026-08-31 08:30:00", ticket.resolutionDate());
    }

    @Test
    void createBody_dateEResolutionDateUsamOMesmoFormato() {
        GlpiRequestDTO ticket = ticketDe(auditorium());

        assertTrue(ticket.date().matches(FORMATO_GLPI),
                "date fora do formato do GLPI: " + ticket.date());
        assertTrue(ticket.resolutionDate().matches(FORMATO_GLPI),
                "resolution_date fora do formato do GLPI: " + ticket.resolutionDate());
    }

    @Test
    void createBody_nameTicketSeparaLabelDataEHora() {
        GlpiRequestDTO ticket = ticketDe(auditorium());

        assertEquals("Acompanhamento no Auditório - 31/08 às 08:30", ticket.name());
    }

    @Test
    void createBody_contentUsaTelefoneEmailEPublicoExterno() {
        GlpiRequestDTO ticket = ticketDe(auditorium());

        assertTrue(ticket.content().contains("TEL REPONSÁVEL: 1140028922"), ticket.content());
        assertTrue(ticket.content().contains("EMAIL RESPONSÁVEL: solicitante@teste.com"), ticket.content());
        assertTrue(ticket.content().contains("QUANTAS PESSOAS? 10"), ticket.content());
    }

    @Test
    void createBody_semUsuarioTelefoneOuPublicoExterno_naoQuebra() {
        AuditoriumEntity entity = auditorium();
        entity.setUser(null);
        entity.setTelephone(null);
        entity.setNumExternalPeople(null);

        GlpiRequestDTO ticket = ticketDe(entity);

        assertTrue(ticket.content().contains("TEL REPONSÁVEL: Não informado"), ticket.content());
        assertTrue(ticket.content().contains("EMAIL RESPONSÁVEL: Não informado"), ticket.content());
        assertTrue(ticket.content().contains("QUANTAS PESSOAS? Não informado"), ticket.content());
    }

    @Test
    void createBody_semPublicoExterno_naoPerguntaQuantasPessoas() {
        AuditoriumEntity entity = auditorium();
        entity.setExternalPublic(false);

        GlpiRequestDTO ticket = ticketDe(entity);

        assertFalse(ticket.content().contains("QUANTAS PESSOAS?"), ticket.content());
        assertTrue(ticket.content().contains("HAVERÁ PÚBLICO EXTERNO? Não"), ticket.content());
    }

    @Test
    void createBody_contentLevaAObservacaoDaReserva() {
        AuditoriumEntity entity = auditorium();
        entity.setObservation("Precisa de 2 microfones sem fio");

        GlpiRequestDTO ticket = ticketDe(entity);

        assertTrue(ticket.content().contains("OBSERVAÇÕES: Precisa de 2 microfones sem fio"), ticket.content());
    }

    @Test
    void createBody_semObservacao_escreveNenhuma() {
        GlpiRequestDTO ticket = ticketDe(auditorium());

        assertTrue(ticket.content().contains("OBSERVAÇÕES: Nenhuma"), ticket.content());
        assertFalse(ticket.content().contains("OBSERVAÇÕES: null"), ticket.content());
    }

    @Test
    void createTicket_reservaInexistente_throws() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.createTicket(999L));
    }

    @Test
    void createBody_aberturaAntesDas8_atribuiParaOUsuario() {
        AuditoriumEntity entity = auditorium();
        entity.setTimeOpenAuditorium(LocalTime.of(7, 30));

        GlpiRequestDTO ticket = ticketDe(entity);

        assertEquals(63L, ticket.usersIdAssign());
        assertNull(ticket.groupsIdAssign(), "antes das 8h o chamado nao vai pro grupo");
    }

    @Test
    void createBody_aberturaAs8EmPonto_atribuiParaOUsuario() {
        AuditoriumEntity entity = auditorium();
        entity.setTimeOpenAuditorium(LocalTime.of(8, 0));

        GlpiRequestDTO ticket = ticketDe(entity);

        assertEquals(63L, ticket.usersIdAssign());
        assertNull(ticket.groupsIdAssign(), "as 8h em ponto o chamado nao vai pro grupo");
    }

    @Test
    void createBody_aberturaDepoisDas8_atribuiParaOGrupo() {
        AuditoriumEntity entity = auditorium();
        entity.setTimeOpenAuditorium(LocalTime.of(8, 30));

        GlpiRequestDTO ticket = ticketDe(entity);

        assertEquals(2L, ticket.groupsIdAssign());
        assertNull(ticket.usersIdAssign(), "depois das 8h o chamado nao vai pra uma pessoa");
    }

    @Test
    void createTicket_gravaOIdDoChamadoNaReserva() {
        AuditoriumEntity entity = auditorium();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        GlpiService spy = spy(service);
        doReturn(new GlpiResponseDTO(4242L, "ok")).when(spy).postTicket(entity);

        spy.createTicket(1L);

        verify(repository).putIdTicket(4242L, 1L);
    }

    @Test
    void createTicket_naoMandaEmail_quemAvisaEOServicoDeReserva() {
        AuditoriumEntity entity = auditorium();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        GlpiService spy = spy(service);
        doReturn(new GlpiResponseDTO(4242L, "ok")).when(spy).postTicket(entity);

        spy.createTicket(1L);

        verify(repository).putIdTicket(4242L, 1L);
        verifyNoInteractions(smtpService);
    }

    @Test
    void createTicket_semUsuarioVinculado_naoQuebra() {
        AuditoriumEntity entity = auditorium();
        entity.setUser(null);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        GlpiService spy = spy(service);
        doReturn(new GlpiResponseDTO(4242L, "ok")).when(spy).postTicket(entity);

        assertDoesNotThrow(() -> spy.createTicket(1L));

        verify(repository).putIdTicket(4242L, 1L);
    }

    @Test
    void getSessionToken_dentroDaValidade_reaproveitaOTokenAtual() {
        ReflectionTestUtils.setField(service, "sessionToken",
                new GlpiSessionTokenDTO("token-em-uso", 19, Instant.now()));

        assertEquals("token-em-uso", service.getSessionToken());
    }

    @Test
    void getSessionToken_noLimiteDaValidade_aindaReaproveita() {
        ReflectionTestUtils.setField(service, "sessionToken",
                new GlpiSessionTokenDTO("token-no-limite", 19, Instant.now().minus(Duration.ofMinutes(19))));

        assertEquals("token-no-limite", service.getSessionToken());
    }
}
