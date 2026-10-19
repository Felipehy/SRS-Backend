package com.siurbinfo.srs.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CognitoGroupsConverterTest {

    private final CognitoGroupsConverter converter = new CognitoGroupsConverter();

    private static Jwt jwtComClaim(Object valorDaClaim) {
        Jwt.Builder builder = Jwt.withTokenValue("token-fake")
                .header("alg", "none")
                .claim("sub", "fulano@prefeitura.sp.gov.br");
        if (valorDaClaim != null) {
            builder.claim("cognito:groups", valorDaClaim);
        }
        return builder.build();
    }

    private static List<String> authorities(Collection<GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    void convert_prefixaCadaGrupoComRole() {
        Collection<GrantedAuthority> result = converter.convert(
                jwtComClaim(List.of("siurb-administrador", "siurb-divisao-frotas")));

        assertEquals(List.of("ROLE_siurb-administrador", "ROLE_siurb-divisao-frotas"), authorities(result));
    }

    @Test
    void convert_umUnicoGrupo() {
        Collection<GrantedAuthority> result = converter.convert(jwtComClaim(List.of("siurb-usuario-geral")));

        assertEquals(List.of("ROLE_siurb-usuario-geral"), authorities(result));
    }

    @Test
    void convert_semClaimDeGrupos_devolveVazio() {
        Collection<GrantedAuthority> result = converter.convert(jwtComClaim(null));

        assertNotNull(result, "nunca pode devolver null: viraria NPE no filtro de autenticacao");
        assertTrue(result.isEmpty());
    }

    @Test
    void convert_claimVazia_devolveVazio() {
        Collection<GrantedAuthority> result = converter.convert(jwtComClaim(List.of()));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
