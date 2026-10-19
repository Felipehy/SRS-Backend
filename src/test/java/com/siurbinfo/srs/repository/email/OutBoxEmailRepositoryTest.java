package com.siurbinfo.srs.repository.email;

import com.siurbinfo.srs.entity.email.EmailEntity;
import com.siurbinfo.srs.enums.EmailStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OutBoxEmailRepositoryTest {

    private static final int LOTE = 20;

    @Autowired
    OutBoxEmailRepository repository;

    private EmailEntity persist(String chave, EmailStatus status, Instant proximaTentativa) {
        EmailEntity email = new EmailEntity();
        email.setRecipient("fulano@prefeitura.sp.gov.br");
        email.setSubject("SOLICITAÇÃO APROVADA");
        email.setHtml("<html><body>corpo</body></html>");
        email.setIdempotencyKey(chave);
        email.setStatus(status);
        email.setNextAttemptAt(proximaTentativa);
        return repository.saveAndFlush(email);
    }

    private EmailEntity pendenteAgora(String chave) {
        return persist(chave, EmailStatus.PENDING, Instant.now().minus(Duration.ofHours(1)));
    }

    private List<Long> idsNaFila() {
        return repository.getOutBoxEmail().stream().map(EmailEntity::getId).toList();
    }

    @Test
    void getOutBoxEmail_trazOsPendentesJaVencidos() {
        Long id = pendenteAgora("123:AUDITORIUM:APROVADA:fulano").getId();

        assertTrue(idsNaFila().contains(id));
    }

    @Test
    void getOutBoxEmail_ignoraOQueJaFoiEnviado() {
        Long id = persist("123:AUDITORIUM:APROVADA:fulano", EmailStatus.SENT,
                Instant.now().minus(Duration.ofHours(1))).getId();

        assertTrue(idsNaFila().stream().noneMatch(id::equals));
    }

    @Test
    void getOutBoxEmail_ignoraOQueFoiDadoComoPerdido() {
        Long id = persist("123:AUDITORIUM:APROVADA:fulano", EmailStatus.DEAD,
                Instant.now().minus(Duration.ofHours(1))).getId();

        assertTrue(idsNaFila().stream().noneMatch(id::equals));
    }

    @Test
    void getOutBoxEmail_ignoraOQueEstaAguardandoAProximaTentativa() {
        Long id = persist("123:AUDITORIUM:APROVADA:fulano", EmailStatus.PENDING,
                Instant.now().plus(Duration.ofHours(1))).getId();

        assertTrue(idsNaFila().stream().noneMatch(id::equals));
    }

    @Test
    void getOutBoxEmail_devolveNaOrdemDeChegada() {
        Long primeiro = pendenteAgora("1:AUDITORIUM:APROVADA:fulano").getId();
        Long segundo = pendenteAgora("2:AUDITORIUM:APROVADA:fulano").getId();

        List<Long> fila = idsNaFila();

        assertTrue(fila.indexOf(primeiro) < fila.indexOf(segundo),
                "a fila tem que sair ordenada por id");
    }

    @Test
    void getOutBoxEmail_pegaNoMaximoUmLotePorRodada() {
        IntStream.rangeClosed(1, LOTE + 5)
                .forEach(i -> pendenteAgora(i + ":AUDITORIUM:APROVADA:fulano"));

        assertEquals(LOTE, repository.getOutBoxEmail().size());
    }

    @Test
    void idempotencyKey_repetida_eRecusadaPeloBanco() {
        pendenteAgora("123:AUDITORIUM:APROVADA:fulano");

        assertThrows(DataIntegrityViolationException.class,
                () -> pendenteAgora("123:AUDITORIUM:APROVADA:fulano"));
    }

    @Test
    void idempotencyKey_mesmoEventoParaDestinatariosDiferentes_convivem() {
        pendenteAgora("123:AUDITORIUM:ENVIADA_PARA_ANALISE:fulano@prefeitura.sp.gov.br");
        pendenteAgora("123:AUDITORIUM:ENVIADA_PARA_ANALISE:reserva@prefeitura.sp.gov.br");

        assertEquals(2, repository.getOutBoxEmail().size());
    }

    @Test
    void linhaNova_nasceProntaParaOWorker() {
        EmailEntity email = new EmailEntity();
        email.setRecipient("fulano@prefeitura.sp.gov.br");
        email.setSubject("SOLICITAÇÃO APROVADA");
        email.setHtml("<html><body>corpo</body></html>");
        email.setIdempotencyKey("123:AUDITORIUM:APROVADA:fulano");

        EmailEntity salvo = repository.saveAndFlush(email);

        assertEquals(EmailStatus.PENDING, salvo.getStatus());
        assertEquals(0, salvo.getAttempts());
        assertNotNull(salvo.getNextAttemptAt(), "sem next_attempt_at a linha nunca e lida");
    }
}
