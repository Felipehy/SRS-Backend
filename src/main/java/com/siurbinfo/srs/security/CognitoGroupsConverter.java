package com.siurbinfo.srs.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Converte os grupos do Cognito (claim "cognito:groups" do JWT) em authorities do Spring Security.
 * Cada grupo vira uma role no formato "ROLE_<grupo>", usada nas checagens de autorizacao.
 */
public class CognitoGroupsConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    // Le a claim de grupos do Cognito e mapeia cada grupo para uma GrantedAuthority "ROLE_<grupo>"
    @Override
    public Collection<GrantedAuthority> convert(Jwt source) {
        List<String> groups = source.getClaimAsStringList("cognito:groups");
        if (groups == null || groups.isEmpty()){
            return List.of();
        }
        return groups.stream()
                .<GrantedAuthority>map(group -> new SimpleGrantedAuthority("ROLE_" + group))
                .collect(Collectors.toList());
    }
}
