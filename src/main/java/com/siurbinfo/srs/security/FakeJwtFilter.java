package com.siurbinfo.srs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Component
@Profile("dev")
public class FakeJwtFilter extends OncePerRequestFilter {

    private final String email;
    private final List<String> groups;

    public FakeJwtFilter(@Value("${app.fake-user.email}") String email,
                         @Value("${app.fake-user.groupCognito}") List<String> groups){
        this.email = email;
        this.groups = groups;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        Jwt jwt = Jwt.withTokenValue("local-fake-token")
                .header("alg","none")
                .subject(email)
                .claim("username",email)
                .claim("email",email)
                .claim("cognito:groups",groups)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        Collection<GrantedAuthority> authorities = new CognitoGroupsConverter().convert(jwt);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt,authorities,email));

        filterChain.doFilter(request, response);
    }
}
