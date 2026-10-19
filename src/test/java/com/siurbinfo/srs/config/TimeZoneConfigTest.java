package com.siurbinfo.srs.config;

import jakarta.validation.Configuration;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.FutureOrPresent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeZoneConfigTest {

    private ValidatorFactory factory;

    record Reserva(@FutureOrPresent LocalDate reservationDate) {}

    @BeforeEach
    void setUp() {
        Configuration<?> configuration = Validation.byDefaultProvider().configure();
        new TimeZoneConfig().saoPauloClockCustomizer().customize(configuration);
        factory = configuration.buildValidatorFactory();
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    void relogioDoBeanValidationFicaEmSaoPaulo() {
        assertEquals(ZoneId.of("America/Sao_Paulo"), factory.getClockProvider().getClock().getZone());
    }

    @Test
    void futureOrPresent_hojeEmSaoPaulo_naoTemViolacao() {
        assertTrue(factory.getValidator().validate(new Reserva(LocalDate.now(TimeZoneConfig.ZONE))).isEmpty());
    }

    @Test
    void futureOrPresent_ontemEmSaoPaulo_temViolacao() {
        assertEquals(1, factory.getValidator()
                .validate(new Reserva(LocalDate.now(TimeZoneConfig.ZONE).minusDays(1))).size());
    }
}
