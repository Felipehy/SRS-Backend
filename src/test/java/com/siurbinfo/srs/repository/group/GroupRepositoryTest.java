package com.siurbinfo.srs.repository.group;

import com.siurbinfo.srs.entity.group.GroupEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GroupRepositoryTest {

    private static final String GROUP_COGNITO = "siurb-teste-repo";

    @Autowired
    GroupRepository repository;

    private GroupEntity persistGroup() {
        GroupEntity group = new GroupEntity();
        group.setGroupName("Teste Repo");
        group.setGroupCognito(GROUP_COGNITO);
        group.setColorGroup("#123456");
        group.setDescription("Grupo criado pelo teste");
        return repository.saveAndFlush(group);
    }

    @Test
    void getGroups_devolveChavesQueOServiceConsome() {
        persistGroup();

        List<Map<String, String>> rows = repository.getGroups();

        Map<String, String> row = rows.stream()
                .filter(r -> GROUP_COGNITO.equals(r.get("groupCognito")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "nenhuma linha com a chave 'groupCognito'; chaves devolvidas: "
                                + (rows.isEmpty() ? "<nenhuma linha>" : rows.getFirst().keySet())));

        assertEquals("Teste Repo", row.get("groupName"));
        assertEquals("#123456", row.get("colorGroup"));
        assertEquals("Grupo criado pelo teste", row.get("description"));
    }

    @Test
    void getGroups_trazOsGruposPadraoDaMigracao() {
        List<String> cognitoNames = repository.getGroups().stream()
                .map(r -> r.get("groupCognito"))
                .toList();

        assertTrue(cognitoNames.contains("siurb-administrador"),
                "grupos devolvidos: " + cognitoNames);
        assertTrue(cognitoNames.contains("siurb-usuario-geral"),
                "grupos devolvidos: " + cognitoNames);
    }

    @Test
    void getGroupByGroupCognito_devolveEntidadeCompleta() {
        persistGroup();

        GroupEntity found = repository.getGroupByGroupCognito(GROUP_COGNITO);

        assertNotNull(found, "grupo nao encontrado por group_cognito");
        assertEquals("Teste Repo", found.getGroupName());
        assertEquals(GROUP_COGNITO, found.getGroupCognito());
        assertEquals("#123456", found.getColorGroup());
        assertEquals("Grupo criado pelo teste", found.getDescription());
    }

    @Test
    void getGroupByGroupCognito_inexistente_devolveNull() {
        assertNull(repository.getGroupByGroupCognito("siurb-nao-existe"));
    }
}
