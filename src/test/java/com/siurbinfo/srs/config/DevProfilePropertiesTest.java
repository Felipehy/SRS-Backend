package com.siurbinfo.srs.config;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DevProfilePropertiesTest {

    private static PropertySource<?> devProperties;

    @BeforeAll
    static void loadYaml() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application-dev", new ClassPathResource("application-dev.yaml"));
        assertFalse(sources.isEmpty(), "application-dev.yaml nao foi encontrado ou esta vazio");
        devProperties = sources.getFirst();
    }

    private static Object value(String key) {
        Object value = devProperties.getProperty(key);
        assertNotNull(value, "propriedade ausente no application-dev.yaml: " + key);
        return value;
    }

    @Test
    void propriedadesDoFakeJwtFilterEstaoDefinidas() {
        assertNotNull(value("app.fake-user.email"));
        assertNotNull(value("app.fake-user.groupCognito"));
    }

    @Test
    void propriedadesDoDevCognitoUserServiceEstaoDefinidas() {
        assertNotNull(value("app.fake-user.given-name"));
        assertNotNull(value("app.fake-user.family-name"));
    }

    @Test
    void propriedadesDoDevCognitoGroupServiceEstaoDefinidas() {
        assertNotNull(value("app.fake-user.groupName"));
        assertNotNull(value("app.fake-user.colorGroup"));
        assertNotNull(value("app.fake-user.description"));
    }

    @Test
    void colorGroupNaoFoiEngolidoPeloComentarioDoYaml() {
        assertEquals("#ffffff", value("app.fake-user.colorGroup"),
                "a cor precisa estar entre aspas no yaml, senao o # inicia um comentario");
    }
}
