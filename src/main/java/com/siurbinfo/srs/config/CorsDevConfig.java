package com.siurbinfo.srs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracao de CORS ativa apenas no perfil "dev".
 * Libera as origens usadas em ambiente de desenvolvimento/homologacao.
 */
@Configuration
@Profile("dev")
public class CorsDevConfig implements WebMvcConfigurer {

    // Define as regras de CORS para todas as rotas ("/**") em ambiente dev
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://10.68.100.68:5173", "http://10.68.100.68:8080")
                .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS")
                .allowedHeaders("*");
    }
}
