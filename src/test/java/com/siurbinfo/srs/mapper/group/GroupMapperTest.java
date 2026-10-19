package com.siurbinfo.srs.mapper.group;

import com.siurbinfo.srs.dto.cognito.group.GroupRequest;
import com.siurbinfo.srs.entity.group.GroupEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GroupMapperTest {

    private final GroupMapper mapper = new GroupMapperImpl();

    @Test
    void toEntity_copiaTodosOsCampos() {
        GroupEntity entity = mapper.toEntity(
                new GroupRequest("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos"));

        assertEquals("Frota", entity.getGroupName());
        assertEquals("siurb-frota", entity.getGroupCognito());
        assertEquals("#ff0000", entity.getColorGroup());
        assertEquals("Reserva de veiculos", entity.getDescription());
    }

    @Test
    void toEntity_nulo_devolveNull() {
        assertNull(mapper.toEntity(null));
    }

    @Test
    void toEntity_naoPulaCamposEmBranco() {
        GroupEntity entity = mapper.toEntity(new GroupRequest("Frota", "siurb-frota", "", null));

        assertEquals("", entity.getColorGroup());
        assertNull(entity.getDescription());
    }

    @Test
    void updateGroup_sobrescreveOsCamposDaEntidade() {
        GroupEntity entity = new GroupEntity();
        entity.setGroupName("Nome antigo");
        entity.setGroupCognito("siurb-frota");
        entity.setColorGroup("#000000");
        entity.setDescription("Descricao antiga");

        mapper.updateGroup(
                new GroupRequest("Nome novo", "siurb-frota", "#ffffff", "Descricao nova"), entity);

        assertEquals("Nome novo", entity.getGroupName());
        assertEquals("siurb-frota", entity.getGroupCognito());
        assertEquals("#ffffff", entity.getColorGroup());
        assertEquals("Descricao nova", entity.getDescription());
    }

    @Test
    void updateGroup_camposNulos_mantemOsValoresDaEntidade() {
        GroupEntity entity = new GroupEntity();
        entity.setGroupName("Nome antigo");
        entity.setGroupCognito("siurb-frota");
        entity.setColorGroup("#000000");
        entity.setDescription("Descricao antiga");

        mapper.updateGroup(new GroupRequest(null, "siurb-frota", "#ffffff", null), entity);

        assertEquals("Nome antigo", entity.getGroupName());
        assertEquals("siurb-frota", entity.getGroupCognito());
        assertEquals("#ffffff", entity.getColorGroup());
        assertEquals("Descricao antiga", entity.getDescription());
    }

    @Test
    void updateGroup_camposEmBranco_mantemOsValoresDaEntidade() {
        GroupEntity entity = new GroupEntity();
        entity.setGroupName("Nome antigo");
        entity.setGroupCognito("siurb-frota");
        entity.setColorGroup("#000000");
        entity.setDescription("Descricao antiga");

        mapper.updateGroup(new GroupRequest("", "siurb-frota", "   ", ""), entity);

        assertEquals("Nome antigo", entity.getGroupName());
        assertEquals("siurb-frota", entity.getGroupCognito());
        assertEquals("#000000", entity.getColorGroup());
        assertEquals("Descricao antiga", entity.getDescription());
    }

    @Test
    void updateGroup_dtoNulo_naoAlteraAEntidade() {
        GroupEntity entity = new GroupEntity();
        entity.setGroupName("Nome antigo");

        mapper.updateGroup(null, entity);

        assertEquals("Nome antigo", entity.getGroupName());
    }
}
