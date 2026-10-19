package com.siurbinfo.srs.repository.config;

import com.siurbinfo.srs.entity.config.ConfigEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConfigRepositoryTest {

    @Autowired
    ConfigRepository repository;

    private ConfigEntity persist(boolean ativo) {
        ConfigEntity config = new ConfigEntity();
        config.setActiveSmt(ativo);
        config.setUpdatedAt(Instant.now());
        return repository.saveAndFlush(config);
    }

    @Test
    void migration_semeiaALinhaQueOSetIsActiveSmtpAltera() {
        assertTrue(repository.findById(1L).isPresent(), "a V41 nao semeou a linha de id 1");
        assertNotNull(repository.getLastModificationSmtp());
    }

    @Test
    void getLastModificationSmtp_refleteAAlteracaoNaLinhaSemeada() {
        ConfigEntity semeada = repository.findById(1L).orElseThrow();
        semeada.setActiveSmt(false);
        semeada.setUpdatedAt(Instant.now());
        repository.saveAndFlush(semeada);

        assertFalse(repository.getLastModificationSmtp(),
                "desligou na linha 1 e a leitura do worker nao viu");

        semeada.setActiveSmt(true);
        repository.saveAndFlush(semeada);

        assertTrue(repository.getLastModificationSmtp());
    }

    @Test
    void getLastModificationSmtp_leALinhaDeMaiorId() {
        persist(false);
        ConfigEntity ultima = persist(true);

        assertTrue(repository.getLastModificationSmtp());

        ultima.setActiveSmt(false);
        repository.saveAndFlush(ultima);

        assertEquals(Boolean.FALSE, repository.getLastModificationSmtp());
    }
}
