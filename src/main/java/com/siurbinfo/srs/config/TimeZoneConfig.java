package com.siurbinfo.srs.config;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeZoneConfig {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    ValidationConfigurationCustomizer saoPauloClockCustomizer() {
        return configuration -> configuration.clockProvider(() -> Clock.system(ZONE));
    }
}
