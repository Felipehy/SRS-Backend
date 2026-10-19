package com.siurbinfo.srs.service.cognito.group;

import com.siurbinfo.srs.dto.cognito.group.GroupRequest;
import com.siurbinfo.srs.dto.cognito.group.GroupResponse;
import com.siurbinfo.srs.dto.cognito.group.GroupResponseMessage;
import com.siurbinfo.srs.dto.cognito.group.ListUsersInGroupResponseDTO;
import com.siurbinfo.srs.dto.cognito.group.UserGroupsRequest;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.entity.group.GroupEntity;
import com.siurbinfo.srs.exception.EmptyRequestException;
import com.siurbinfo.srs.exception.GroupNotFoundedException;
import com.siurbinfo.srs.mapper.group.GroupMapper;
import com.siurbinfo.srs.repository.group.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DevCognitoGroupServiceTest {

    private static final GroupRequest GROUP =
            new GroupRequest("Administrador", "siurb-administrador", "#ffffff", "Grupo dev");

    private DevCognitoGroupService service;
    private GroupRepository groupRepository;
    private GroupMapper groupMapper;

    @BeforeEach
    void setUp() {
        groupRepository = mock(GroupRepository.class);
        groupMapper = mock(GroupMapper.class);
        service = new DevCognitoGroupService(
                groupRepository,
                groupMapper,
                List.of("siurb-administrador", "siurb-usuario-geral"),
                "admin",
                "#ffffff",
                "Esse perfil é de dev",
                "Dev",
                "Local");
    }

    private static GroupEntity entity(String groupName, String groupCognito, String color, String description) {
        GroupEntity entity = new GroupEntity();
        entity.setGroupName(groupName);
        entity.setGroupCognito(groupCognito);
        entity.setColorGroup(color);
        entity.setDescription(description);
        return entity;
    }

    @Test
    void listGroup_mapeiaLinhasDoBanco() {
        when(groupRepository.getGroups()).thenReturn(List.of(
                Map.of("groupName", "Administrador", "groupCognito", "siurb-administrador",
                        "colorGroup", "#ffffff", "description", "Tudo"),
                Map.of("groupName", "Frotas", "groupCognito", "siurb-divisao-frotas",
                        "colorGroup", "#ff0000", "description", "Reservas de Frota")));

        List<GroupResponse> groups = service.listGroup();

        assertEquals(2, groups.size());
        assertEquals(new GroupResponse("Administrador", "siurb-administrador", "#ffffff", "Tudo"),
                groups.getFirst());
        assertEquals("siurb-divisao-frotas", groups.get(1).groupCognito());
    }

    @Test
    void listGroup_semGrupos_devolveListaVazia() {
        when(groupRepository.getGroups()).thenReturn(List.of());

        assertTrue(service.listGroup().isEmpty());
    }

    @Test
    void listGroupsForUser_cruzaGruposFakeComOBanco() {
        when(groupRepository.getGroupByGroupCognito("siurb-administrador"))
                .thenReturn(entity("Administrador", "siurb-administrador", "#ffffff", "Tudo"));
        when(groupRepository.getGroupByGroupCognito("siurb-usuario-geral"))
                .thenReturn(entity("Usuário Geral", "siurb-usuario-geral", "#00ff00", "Cria/vê reservas"));

        List<GroupResponse> groups = service.listGroupsForUser("dev@prefeitura.sp.gov.br");

        assertEquals(List.of(
                new GroupResponse("Administrador", "siurb-administrador", "#ffffff", "Tudo"),
                new GroupResponse("Usuário Geral", "siurb-usuario-geral", "#00ff00", "Cria/vê reservas")), groups);
    }

    @Test
    void listGroupsForUser_grupoForaDoBanco_caiNoFallbackDoYaml() {
        when(groupRepository.getGroupByGroupCognito(anyString())).thenReturn(null);

        List<GroupResponse> groups = service.listGroupsForUser("dev@prefeitura.sp.gov.br");

        assertEquals(new GroupResponse("admin", "siurb-administrador", "#ffffff", "Esse perfil é de dev"),
                groups.getFirst());
    }

    @Test
    void listUsersInGroup_grupoDoUsuarioFake_devolveOUsuarioFake() {
        ListUsersInGroupResponseDTO response = service.listUsersInGroup(GROUP);

        assertEquals("Administrador", response.group());
        assertEquals(1, response.username().size());
        assertEquals("Dev", response.username().getFirst().get("name"));
        assertEquals("Local", response.username().getFirst().get("familyName"));
    }

    @Test
    void listUsersInGroup_grupoSemOUsuarioFake_devolveVazio() {
        ListUsersInGroupResponseDTO response = service.listUsersInGroup(
                new GroupRequest("Frotas", "siurb-divisao-frotas", "#ff0000", "Reservas de Frota"));

        assertEquals("Frotas", response.group());
        assertTrue(response.username().isEmpty());
    }

    @Test
    void createGroup_salvaNoBancoSemChamarCognito() {
        GroupEntity mapped = entity("Novo", "siurb-novo", "#0000ff", "Grupo novo");
        GroupRequest dto = new GroupRequest("Novo", "siurb-novo", "#0000ff", "Grupo novo");
        when(groupMapper.toEntity(dto)).thenReturn(mapped);

        GroupResponse response = service.createGroup(dto);

        assertEquals(new GroupResponse("Novo", "siurb-novo", "#0000ff", "Grupo novo"), response);
        verify(groupRepository).save(mapped);
    }

    @Test
    void deleteGroup_apagaNoBanco() {
        GroupResponseMessage response = service.deleteGroup(GROUP);

        assertEquals("siurb-administrador", response.groupName());
        assertEquals("Grupo deletado com sucesso!", response.message());
        verify(groupRepository).deleteByGroupCognito("siurb-administrador");
    }

    @Test
    void deleteGroup_groupCognitoEmBranco_lancaEmptyRequestException() {
        GroupRequest dto = new GroupRequest("Administrador", "", "#ffffff", "Grupo dev");

        assertThrows(EmptyRequestException.class, () -> service.deleteGroup(dto));
        verifyNoInteractions(groupRepository);
    }

    @Test
    void updateGroup_atualizaALinhaDoBanco() {
        GroupEntity existente = entity("Administrador", "siurb-administrador", "#ffffff", "Descricao antiga");
        when(groupRepository.getGroupByGroupCognito("siurb-administrador")).thenReturn(existente);

        GroupResponseMessage response = service.updateGroup(GROUP);

        assertEquals("Administrador", response.groupName());
        assertEquals("O grupo foi atualizado", response.message());
        verify(groupMapper).updateGroup(GROUP, existente);
        verify(groupRepository).save(existente);
    }

    @Test
    void updateGroup_groupCognitoEmBranco_lancaEmptyRequestException() {
        GroupRequest dto = new GroupRequest("Administrador", "", "#ffffff", "Grupo dev");

        assertThrows(EmptyRequestException.class, () -> service.updateGroup(dto));
        verifyNoInteractions(groupRepository);
    }

    @Test
    void updateGroup_grupoInexistenteNoBanco_lancaGroupNotFounded() {
        when(groupRepository.getGroupByGroupCognito("siurb-administrador")).thenReturn(null);

        assertThrows(GroupNotFoundedException.class, () -> service.updateGroup(GROUP));
        verify(groupRepository, never()).save(any(GroupEntity.class));
    }

    @Test
    void vinculoDeUsuarioComGrupo_naoESuportadoNoPerfilDev() {
        UserGroupsRequest userGroup = new UserGroupsRequest("dev@prefeitura.sp.gov.br", "siurb-administrador");

        assertThrows(UnsupportedOperationException.class, () -> service.addUserToGroups(userGroup));
        assertThrows(UnsupportedOperationException.class, () -> service.removeUserFromGroups(userGroup));
    }
}
