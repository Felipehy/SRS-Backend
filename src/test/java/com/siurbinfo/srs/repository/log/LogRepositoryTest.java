package com.siurbinfo.srs.repository.log;

import com.siurbinfo.srs.entity.log.LogEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LogRepositoryTest {

    private static final String EMAIL = "log@teste.com";

    @Autowired
    LogRepository repository;

    @Autowired
    UserRepository userRepository;

    private UserEntity persistUser() {
        UserEntity user = new UserEntity();
        user.setEmail(EMAIL);
        user.setGivenName("Fulano");
        user.setFamilyName("de Tal");
        return userRepository.saveAndFlush(user);
    }

    private LogEntity persistLog(Long userId) {
        LogEntity log = new LogEntity();
        log.setModification("criou reserva");
        log.setLocalModification("auditorio");
        log.setReservationType(ReserveType.AUDITORIUM);
        log.setIdData(42L);
        log.setLogsType(LogsType.CREATE);
        log.setUser(userId);
        log.setGroupUser(List.of("siurb-administrador"));
        return repository.saveAndFlush(log);
    }

    @Test
    void save_gravaListaDeGruposERecuperaDeVolta() {
        LogEntity salvo = persistLog(persistUser().getId());

        LogEntity lido = repository.findById(salvo.getId()).orElseThrow();

        assertEquals(List.of("siurb-administrador"), lido.getGroupUser());
        assertEquals(LogsType.CREATE, lido.getLogsType());
        assertEquals(ReserveType.AUDITORIUM, lido.getReservationType());
        assertNotNull(lido.getModificationDate());
    }

    @Test
    void getLogs_projecaoTrazTodosOsCamposDoLogEDoUsuario() {
        Long userId = persistUser().getId();
        LogEntity salvo = persistLog(userId);

        LogProjection row = repository.getLogs(0L).stream()
                .filter(p -> salvo.getId().equals(p.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("log recem gravado nao veio no getLogs(0)"));

        assertEquals("criou reserva", row.getModification());
        assertEquals("auditorio", row.getLocalModification());
        assertEquals("AUDITORIUM", row.getReservationType());
        assertEquals("CREATE", row.getTypeLog());
        assertEquals(42L, row.getIdData());
        assertNotNull(row.getModificationDate());
        assertNotNull(row.getGroupUser());
        assertEquals("Fulano", row.getGivenName());
        assertEquals("de Tal", row.getFamilyName());
        assertEquals(EMAIL, row.getEmail());
    }

    @Test
    void getLogs_semUsuarioVinculado_naoPerdeALinha() {
        LogEntity salvo = persistLog(null);

        LogProjection row = repository.getLogs(0L).stream()
                .filter(p -> salvo.getId().equals(p.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("log sem usuario sumiu do getLogs"));

        assertNull(row.getEmail());
        assertEquals("criou reserva", row.getModification());
    }

    @Test
    void getLogs_devolveNoMaximoDezPorPagina() {
        Long userId = persistUser().getId();
        for (int i = 0; i < 12; i++) {
            persistLog(userId);
        }

        assertTrue(repository.getLogs(0L).size() <= 10);
    }

    @Test
    void getLogs_offsetPulaOsPrimeirosRegistros() {
        Long userId = persistUser().getId();
        for (int i = 0; i < 12; i++) {
            persistLog(userId);
        }

        List<Long> primeiraPagina = repository.getLogs(0L).stream().map(LogProjection::getId).toList();
        List<Long> segundaPagina = repository.getLogs(10L).stream().map(LogProjection::getId).toList();

        assertFalse(primeiraPagina.isEmpty());
        assertTrue(segundaPagina.stream().noneMatch(primeiraPagina::contains),
                "offset nao pulou a primeira pagina");
    }

    @Test
    void getLogs_offsetAlemDoFim_devolveListaVazia() {
        assertTrue(repository.getLogs(1_000_000L).isEmpty());
    }
}
