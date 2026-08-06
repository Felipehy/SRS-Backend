package com.siurbinfo.srs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuracao de CORS ativa apenas no perfil "prod".
 * Libera somente a origem oficial do front-end em producao.
 */
@Configuration
@Profile("prod")
public class CorsProdConfig implements WebMvcConfigurer {

    // Define as regras de CORS para todas as rotas ("/**") em producao
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("https://srs.siurbinfo.com")
                .allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS");
    }
}
