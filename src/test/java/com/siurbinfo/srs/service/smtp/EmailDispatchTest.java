package com.siurbinfo.srs.service.smtp;

import com.siurbinfo.srs.entity.email.EmailEntity;
import com.siurbinfo.srs.enums.EmailStatus;
import com.siurbinfo.srs.exception.NotHasSendEmailException;
import com.siurbinfo.srs.repository.email.OutBoxEmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmailDispatchTest {

    private static final int MAX_TENTATIVAS = 5;

    private OutBoxEmailRepository repository;
    private SmtpService smtpService;
    private EmailDispatch dispatch;

    @BeforeEach
    void setUp() {
        repository = mock(OutBoxEmailRepository.class);
        smtpService = mock(SmtpService.class);
        when(smtpService.getIsActiveSmtp()).thenReturn(true);
        dispatch = new EmailDispatch(repository, smtpService);
    }

    private EmailEntity pendente(String destinatario) {
        EmailEntity email = new EmailEntity();
        email.setId(1L);
        email.setRecipient(destinatario);
        email.setSubject("SOLICITAÇÃO APROVADA");
        email.setHtml("<html><body>corpo</body></html>");
        email.setIdempotencyKey("123:AUDITORIUM:APROVADA:" + destinatario);
        return email;
    }

    private void naFila(EmailEntity... emails) {
        when(repository.getOutBoxEmail()).thenReturn(List.of(emails));
    }

    @Test
    void dispatch_entregaOQueEstaNaFila() {
        EmailEntity email = pendente("fulano@prefeitura.sp.gov.br");
        naFila(email);

        dispatch.dispatch();

        verify(smtpService).send("fulano@prefeitura.sp.gov.br", "SOLICITAÇÃO APROVADA",
                "<html><body>corpo</body></html>");
    }

    @Test
    void dispatch_entregue_marcaComoEnviado() {
        EmailEntity email = pendente("fulano@prefeitura.sp.gov.br");
        naFila(email);

        dispatch.dispatch();

        assertEquals(EmailStatus.SENT, email.getStatus());
        assertEquals(0, email.getAttempts(), "entrega no primeiro try nao conta tentativa");
        assertNull(email.getLastError());
    }

    @Test
    void dispatch_filaVazia_naoFalaComOSmtp() {
        when(repository.getOutBoxEmail()).thenReturn(List.of());

        dispatch.dispatch();

        verify(smtpService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void dispatch_smtpDesligado_naoEnviaNada() {
        when(smtpService.getIsActiveSmtp()).thenReturn(false);
        naFila(pendente("fulano@prefeitura.sp.gov.br"));

        dispatch.dispatch();

        verify(smtpService, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void dispatch_smtpDesligado_naoTocaNaFila() {
        when(smtpService.getIsActiveSmtp()).thenReturn(false);
        EmailEntity email = pendente("fulano@prefeitura.sp.gov.br");
        naFila(email);

        dispatch.dispatch();

        verifyNoInteractions(repository);
        assertEquals(EmailStatus.PENDING, email.getStatus());
        assertEquals(0, email.getAttempts());
    }

    @Test
    void dispatch_smtpReligado_entregaOQueFicouParado() {
        EmailEntity email = pendente("fulano@prefeitura.sp.gov.br");
        naFila(email);
        when(smtpService.getIsActiveSmtp()).thenReturn(false, true);

        dispatch.dispatch();
        assertEquals(EmailStatus.PENDING, email.getStatus());

        dispatch.dispatch();
        assertEquals(EmailStatus.SENT, email.getStatus());
    }

    @Test
    void dispatch_falhaNoEnvio_contaTentativaEGuardaOErro() {
        EmailEntity email = pendente("fulano@prefeitura.sp.gov.br");
        naFila(email);
        doThrow(new NotHasSendEmailException("caixa cheia"))
                .when(smtpService).send(anyString(), anyString(), anyString());

        dispatch.dispatch();

        assertEquals(1, email.getAttempts());
        assertEquals("caixa cheia", email.getLastError());
    }

    @Test
    void dispatch_falhaNoEnvio_continuaPendenteParaOProximoCiclo() {
        EmailEntity email = pendente("fulano@prefeitura.sp.gov.br");
        naFila(email);
        doThrow(new NotHasSendEmailException("caixa cheia"))
                .when(smtpService).send(anyString(), anyString(), anyString());

        dispatch.dispatch();

        assertEquals(EmailStatus.PENDING, email.getStatus());
    }

    @Test
    void dispatch_falhaNoEnvio_adiaAProximaTentativaDobrandoOIntervalo() {
        EmailEntity email = pendente("fulano@prefeitura.sp.gov.br");
        email.setAttempts(2);
        naFila(email);
        doThrow(new NotHasSendEmailException("caixa cheia"))
                .when(smtpService).send(anyString(), anyString(), anyString());

        Instant antes = Instant.now();
        dispatch.dispatch();

        assertNotNull(email.getNextAttemptAt());
        Duration espera = Duration.between(antes, email.getNextAttemptAt());
        assertTrue(espera.toMinutes() >= 7 && espera.toMinutes() <= 8,
                "3a tentativa devia esperar ~8 min, esperou " + espera);
    }

    @Test
    void dispatch_mailExceptionCrua_tambemEtratada() {
        EmailEntity email = pendente("fulano@prefeitura.sp.gov.br");
        naFila(email);
        doThrow(new MailSendException("smtp fora do ar"))
                .when(smtpService).send(anyString(), anyString(), anyString());

        dispatch.dispatch();

        assertEquals(1, email.getAttempts());
        assertEquals(EmailStatus.PENDING, email.getStatus());
    }

    @Test
    void dispatch_noLimiteDeTentativas_marcaComoDead() {
        EmailEntity email = pendente("fulano@prefeitura.sp.gov.br");
        email.setAttempts(MAX_TENTATIVAS - 1);
        naFila(email);
        doThrow(new NotHasSendEmailException("caixa cheia"))
                .when(smtpService).send(anyString(), anyString(), anyString());

        dispatch.dispatch();

        assertEquals(MAX_TENTATIVAS, email.getAttempts());
        assertEquals(EmailStatus.DEAD, email.getStatus());
    }

    @Test
    void dispatch_antesDoLimite_naoDesiste() {
        EmailEntity email = pendente("fulano@prefeitura.sp.gov.br");
        email.setAttempts(MAX_TENTATIVAS - 2);
        naFila(email);
        doThrow(new NotHasSendEmailException("caixa cheia"))
                .when(smtpService).send(anyString(), anyString(), anyString());

        dispatch.dispatch();

        assertEquals(EmailStatus.PENDING, email.getStatus());
    }

    @Test
    void dispatch_umEnderecoRuimNaoSeguraOResto() {
        EmailEntity ruim = pendente("nao-existe@prefeitura.sp.gov.br");
        EmailEntity bom = pendente("fulano@prefeitura.sp.gov.br");
        naFila(ruim, bom);

        doThrow(new NotHasSendEmailException("endereço inválido"))
                .when(smtpService).send(eq("nao-existe@prefeitura.sp.gov.br"), anyString(), anyString());
        doNothing().when(smtpService).send(eq("fulano@prefeitura.sp.gov.br"), anyString(), anyString());

        dispatch.dispatch();

        assertEquals(EmailStatus.PENDING, ruim.getStatus());
        assertEquals(1, ruim.getAttempts());
        assertEquals(EmailStatus.SENT, bom.getStatus(), "o segundo e-mail nao chegou a ser tentado");
    }

    @Test
    void dispatch_erroInesperado_naoEEngolido() {
        EmailEntity email = pendente("fulano@prefeitura.sp.gov.br");
        naFila(email);
        doThrow(new IllegalStateException("linha sem html"))
                .when(smtpService).send(anyString(), anyString(), anyString());

        assertThrows(IllegalStateException.class, () -> dispatch.dispatch());
    }
}
