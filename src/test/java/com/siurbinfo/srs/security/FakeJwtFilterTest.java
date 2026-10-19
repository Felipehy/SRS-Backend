package com.siurbinfo.srs.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FakeJwtFilterTest {

    private static final String EMAIL = "dev@prefeitura.sp.gov.br";

    @AfterEach
    void limpaContexto() {
        SecurityContextHolder.clearContext();
    }

    private static Authentication autenticaCom(List<String> grupos) throws Exception {
        FakeJwtFilter filter = new FakeJwtFilter(EMAIL, grupos);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void doFilter_autenticaComOsGruposConfigurados() throws Exception {
        Authentication auth = autenticaCom(List.of("siurb-administrador", "siurb-divisao-frotas"));

        assertNotNull(auth, "a requisicao ficou sem autenticacao no perfil dev");
        assertInstanceOf(JwtAuthenticationToken.class, auth);
        assertEquals(List.of("ROLE_siurb-administrador", "ROLE_siurb-divisao-frotas"),
                auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }

    @Test
    void doFilter_montaJwtComEmailEGrupos() throws Exception {
        Authentication auth = autenticaCom(List.of("siurb-administrador"));

        Jwt jwt = (Jwt) auth.getPrincipal();
        assertEquals(EMAIL, jwt.getSubject());
        assertEquals(EMAIL, jwt.getClaimAsString("email"));
        assertEquals(EMAIL, jwt.getClaimAsString("username"));
        assertEquals(List.of("siurb-administrador"), jwt.getClaimAsStringList("cognito:groups"));
        assertEquals("local-fake-token", jwt.getTokenValue());
        assertNotNull(jwt.getExpiresAt());
        assertTrue(jwt.getExpiresAt().isAfter(jwt.getIssuedAt()));
    }

    @Test
    void doFilter_semGrupos_autenticaSemRoles() throws Exception {
        Authentication auth = autenticaCom(List.of());

        assertNotNull(auth);
        assertTrue(auth.getAuthorities().isEmpty());
    }
}
