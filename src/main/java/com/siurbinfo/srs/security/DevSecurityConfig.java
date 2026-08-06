package com.siurbinfo.srs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracao de seguranca ativa apenas no perfil "dev".
 * Libera todas as requisicoes sem autenticacao para facilitar o desenvolvimento local.
 */
@Configuration
@Profile("dev")
public class DevSecurityConfig {

    // Monta a cadeia de filtros de seguranca do ambiente dev: sem auth e sem csrf
    @Bean
    SecurityFilterChain devFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // qualquer request e liberada em dev
                .csrf(csrf -> csrf.disable()); // csrf desabilitado pois nao ha sessao/formulario tradicional
        return http.build();
    }

}
