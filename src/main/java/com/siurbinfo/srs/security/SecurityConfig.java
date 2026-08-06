package com.siurbinfo.srs.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

/**
 * Configuracao de seguranca ativa no perfil "prod".
 * Protege a API com autenticacao via JWT do Cognito (resource server OAuth2),
 * definindo quais rotas sao publicas e como as roles dos usuarios sao extraidas.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("prod")
public class SecurityConfig {

    /** Header que o ALB (authenticate-cognito) injeta com o access token do Cognito. */
    private static final String ALB_ACCESS_TOKEN_HEADER = "x-amzn-oidc-accesstoken";

    // Monta a cadeia de filtros de seguranca: stateless, JWT do Cognito e rotas publicas de health/diagnostico
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(new CognitoGroupsConverter());

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // API stateless, sem sessao HTTP
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // libera preflight de CORS
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(albAwareBearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
                );
        return http.build();
    }

    /**
     * Resolve o Bearer token em duas etapas:
     *   1) header padrao "Authorization: Bearer <token>" (ex.: injetado pelo nginx);
     *   2) fallback: header "x-amzn-oidc-accesstoken" que o ALB injeta com o access token do Cognito.
     *
     * A validacao (assinatura via JWKS + emissor + expiracao) e a conversao de grupos
     * (CognitoGroupsConverter) continuam identicas - so muda de ONDE o token e lido.
     *
     * Seguro desde que o backend so seja acessivel atraves do ALB: o token continua sendo
     * validado criptograficamente, entao um header forjado sem token Cognito valido e rejeitado.
     */
    private BearerTokenResolver albAwareBearerTokenResolver() {
        DefaultBearerTokenResolver standard = new DefaultBearerTokenResolver();
        return request -> {
            String token = standard.resolve(request);
            if (token != null) {
                return token;
            }
            String albToken = request.getHeader(ALB_ACCESS_TOKEN_HEADER);
            return StringUtils.hasText(albToken) ? albToken : null;
        };
    }
}
