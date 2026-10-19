package com.siurbinfo.srs.service.cognito.group;

import com.siurbinfo.srs.dto.cognito.group.GroupRequest;
import com.siurbinfo.srs.dto.cognito.group.GroupResponse;
import com.siurbinfo.srs.dto.cognito.group.GroupResponseMessage;
import com.siurbinfo.srs.dto.cognito.group.ListUsersInGroupResponseDTO;
import com.siurbinfo.srs.dto.cognito.group.UserGroupsRequest;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.entity.group.GroupEntity;
import com.siurbinfo.srs.exception.EmptyRequestException;
import com.siurbinfo.srs.mapper.group.GroupMapper;
import com.siurbinfo.srs.repository.group.GroupRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CognitoGroupServiceImplTest {

    private static final String ISSUER_URI =
            "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_AbCdEf123";
    private static final String USER_POOL_ID = "us-east-1_AbCdEf123";

    private CognitoGroupServiceImpl service;
    private CognitoIdentityProviderClient client;
    private UserRepository userRepository;
    private GroupRepository groupRepository;
    private GroupMapper groupMapper;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        groupRepository = mock(GroupRepository.class);
        groupMapper = mock(GroupMapper.class);
        service = new CognitoGroupServiceImpl(ISSUER_URI, userRepository, groupRepository, groupMapper);
        client = mock(CognitoIdentityProviderClient.class);
        service.client = client;
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
                Map.of("groupName", "Frota", "groupCognito", "siurb-frota",
                        "colorGroup", "#ff0000", "description", "Reserva de veiculos"),
                Map.of("groupName", "Auditorio", "groupCognito", "siurb-auditorio",
                        "colorGroup", "#00ff00", "description", "Reserva do auditorio")));

        List<GroupResponse> groups = service.listGroup();

        assertEquals(2, groups.size());
        assertEquals(new GroupResponse("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos"), groups.get(0));
        assertEquals("siurb-auditorio", groups.get(1).groupCognito());
        verifyNoInteractions(client);
    }

    @Test
    void listGroup_semGrupos_devolveListaVazia() {
        when(groupRepository.getGroups()).thenReturn(List.of());

        assertTrue(service.listGroup().isEmpty());
    }

    @Test
    void listGroupsForUser_cruzaCognitoComBanco() {
        when(client.adminListGroupsForUser(any(AdminListGroupsForUserRequest.class)))
                .thenReturn(AdminListGroupsForUserResponse.builder()
                        .groups(GroupType.builder().groupName("siurb-frota").build())
                        .build());
        when(groupRepository.getGroupByGroupCognito("siurb-frota"))
                .thenReturn(entity("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos"));

        List<GroupResponse> groups = service.listGroupsForUser("fulano@prefeitura.sp.gov.br");

        assertEquals(List.of(new GroupResponse("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos")), groups);

        ArgumentCaptor<AdminListGroupsForUserRequest> captor =
                ArgumentCaptor.forClass(AdminListGroupsForUserRequest.class);
        verify(client).adminListGroupsForUser(captor.capture());
        assertEquals(USER_POOL_ID, captor.getValue().userPoolId());
        assertEquals("fulano@prefeitura.sp.gov.br", captor.getValue().username());
    }

    @Test
    void listUsersInGroup_mapeiaNomeESobrenome() {
        when(client.listUsersInGroup(any(ListUsersInGroupRequest.class)))
                .thenReturn(ListUsersInGroupResponse.builder()
                        .users(UserType.builder()
                                .attributes(
                                        AttributeType.builder().name("given_name").value("Fulano").build(),
                                        AttributeType.builder().name("family_name").value("de Tal").build(),
                                        AttributeType.builder().name("email").value("fulano@x.gov.br").build())
                                .build())
                        .nextToken(null)
                        .build());

        ListUsersInGroupResponseDTO response = service.listUsersInGroup(
                new GroupRequest("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos"));

        assertEquals("Frota", response.group());
        assertEquals(1, response.username().size());
        assertEquals("Fulano", response.username().get(0).get("name"));
        assertEquals("de Tal", response.username().get(0).get("familyName"));
        assertFalse(response.username().get(0).containsKey("email"));
    }

    @Test
    void listUsersInGroup_paginaAteNextTokenSerNulo() {
        ListUsersInGroupResponse page1 = ListUsersInGroupResponse.builder()
                .users(UserType.builder()
                        .attributes(AttributeType.builder().name("given_name").value("Um").build())
                        .build())
                .nextToken("token-1")
                .build();
        ListUsersInGroupResponse page2 = ListUsersInGroupResponse.builder()
                .users(UserType.builder()
                        .attributes(AttributeType.builder().name("given_name").value("Dois").build())
                        .build())
                .nextToken(null)
                .build();
        when(client.listUsersInGroup(any(ListUsersInGroupRequest.class))).thenReturn(page1, page2);

        ListUsersInGroupResponseDTO response = service.listUsersInGroup(
                new GroupRequest("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos"));

        ArgumentCaptor<ListUsersInGroupRequest> captor =
                ArgumentCaptor.forClass(ListUsersInGroupRequest.class);
        verify(client, times(2)).listUsersInGroup(captor.capture());
        List<ListUsersInGroupRequest> requests = captor.getAllValues();

        assertEquals(USER_POOL_ID, requests.get(0).userPoolId());
        assertNull(requests.get(0).nextToken());
        assertEquals("token-1", requests.get(1).nextToken());
        assertEquals("siurb-frota", requests.get(0).groupName());

        assertEquals(List.of("Um", "Dois"),
                response.username().stream().map(u -> u.get("name")).toList());
    }

    @Test
    void createGroup_criaNoCognitoESalvaNoBanco() {
        GroupRequest dto = new GroupRequest("Novo", "siurb-novo", "#0000ff", "Grupo novo");
        GroupEntity mapped = entity("Novo", "siurb-novo", "#0000ff", "Grupo novo");
        when(groupMapper.toEntity(dto)).thenReturn(mapped);

        GroupResponse response = service.createGroup(dto);

        assertEquals(new GroupResponse("Novo", "siurb-novo", "#0000ff", "Grupo novo"), response);

        ArgumentCaptor<CreateGroupRequest> captor = ArgumentCaptor.forClass(CreateGroupRequest.class);
        verify(client).createGroup(captor.capture());
        assertEquals(USER_POOL_ID, captor.getValue().userPoolId());
        assertEquals("siurb-novo", captor.getValue().groupName());
        assertEquals("Grupo novo", captor.getValue().description());

        verify(groupRepository).save(mapped);
    }

    @Test
    void deleteGroup_apagaNoCognitoENoBanco() {
        GroupResponseMessage response = service.deleteGroup(
                new GroupRequest("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos"));

        assertEquals("siurb-frota", response.groupName());
        assertEquals("Grupo deletado com sucesso!", response.message());

        ArgumentCaptor<DeleteGroupRequest> captor = ArgumentCaptor.forClass(DeleteGroupRequest.class);
        verify(client).deleteGroup(captor.capture());
        assertEquals(USER_POOL_ID, captor.getValue().userPoolId());
        assertEquals("siurb-frota", captor.getValue().groupName());

        verify(groupRepository).deleteByGroupCognito("siurb-frota");
    }

    @Test
    void deleteGroup_groupCognitoEmBranco_lancaEmptyRequestException() {
        GroupRequest dto = new GroupRequest("Frota", "", "#ff0000", "Reserva de veiculos");

        assertThrows(EmptyRequestException.class, () -> service.deleteGroup(dto));
        verifyNoInteractions(client);
        verifyNoInteractions(groupRepository);
    }

    @Test
    void updateGroup_atualizaCognitoEBanco() {
        GroupRequest dto = new GroupRequest("Frota", "siurb-frota", "#ff0000", "Nova descricao");
        GroupEntity existente = entity("Frota", "siurb-frota", "#ff0000", "Descricao antiga");

        when(client.getGroup(any(GetGroupRequest.class)))
                .thenReturn(GetGroupResponse.builder()
                        .group(GroupType.builder()
                                .groupName("siurb-frota")
                                .description("Descricao antiga")
                                .build())
                        .build());
        when(groupRepository.getGroupByGroupCognito("siurb-frota")).thenReturn(existente);

        GroupResponseMessage response = service.updateGroup(dto);

        assertEquals("Frota", response.groupName());
        assertEquals("O grupo foi atualizado", response.message());

        ArgumentCaptor<GetGroupRequest> getCaptor = ArgumentCaptor.forClass(GetGroupRequest.class);
        verify(client).getGroup(getCaptor.capture());
        assertEquals("siurb-frota", getCaptor.getValue().groupName());

        ArgumentCaptor<UpdateGroupRequest> captor = ArgumentCaptor.forClass(UpdateGroupRequest.class);
        verify(client).updateGroup(captor.capture());
        assertEquals(USER_POOL_ID, captor.getValue().userPoolId());
        assertEquals("siurb-frota", captor.getValue().groupName());
        assertEquals("Nova descricao", captor.getValue().description());

        verify(groupMapper).updateGroup(dto, existente);
        verify(groupRepository).save(existente);
    }

    @Test
    void updateGroup_semDescricaoNova_mantemADoCognito() {
        GroupRequest dto = new GroupRequest("Frota", "siurb-frota", "#ff0000", "");

        when(client.getGroup(any(GetGroupRequest.class)))
                .thenReturn(GetGroupResponse.builder()
                        .group(GroupType.builder()
                                .groupName("siurb-frota")
                                .description("Descricao antiga")
                                .build())
                        .build());
        when(groupRepository.getGroupByGroupCognito("siurb-frota"))
                .thenReturn(entity("Frota", "siurb-frota", "#ff0000", "Descricao antiga"));

        service.updateGroup(dto);

        ArgumentCaptor<UpdateGroupRequest> captor = ArgumentCaptor.forClass(UpdateGroupRequest.class);
        verify(client).updateGroup(captor.capture());
        assertEquals("Descricao antiga", captor.getValue().description());
    }

    @Test
    void updateGroup_groupCognitoEmBranco_lancaEmptyRequestException() {
        GroupRequest dto = new GroupRequest("Frota", "", "#ff0000", "Nova descricao");

        assertThrows(EmptyRequestException.class, () -> service.updateGroup(dto));
        verifyNoInteractions(client);
        verifyNoInteractions(groupRepository);
    }

    @Test
    void addUserToGroups_adicionaNoCognitoEDevolveIdDoBanco() {
        when(userRepository.getIdByEmail("fulano@prefeitura.sp.gov.br")).thenReturn(7L);

        UserResponseMessage response = service.addUserToGroups(
                new UserGroupsRequest("fulano@prefeitura.sp.gov.br", "siurb-frota"));

        assertEquals(7L, response.id());
        assertEquals("O usuario foi adicionado ao grupo!", response.message());

        ArgumentCaptor<AdminAddUserToGroupRequest> captor =
                ArgumentCaptor.forClass(AdminAddUserToGroupRequest.class);
        verify(client).adminAddUserToGroup(captor.capture());
        assertEquals(USER_POOL_ID, captor.getValue().userPoolId());
        assertEquals("fulano@prefeitura.sp.gov.br", captor.getValue().username());
        assertEquals("siurb-frota", captor.getValue().groupName());
    }

    @Test
    void removeUserFromGroups_removeNoCognitoEDevolveIdDoBanco() {
        when(userRepository.getIdByEmail("fulano@prefeitura.sp.gov.br")).thenReturn(7L);

        UserResponseMessage response = service.removeUserFromGroups(
                new UserGroupsRequest("fulano@prefeitura.sp.gov.br", "siurb-frota"));

        assertEquals(7L, response.id());
        assertEquals("O usuario foi removido do grupo", response.message());

        ArgumentCaptor<AdminRemoveUserFromGroupRequest> captor =
                ArgumentCaptor.forClass(AdminRemoveUserFromGroupRequest.class);
        verify(client).adminRemoveUserFromGroup(captor.capture());
        assertEquals(USER_POOL_ID, captor.getValue().userPoolId());
        assertEquals("fulano@prefeitura.sp.gov.br", captor.getValue().username());
        assertEquals("siurb-frota", captor.getValue().groupName());
    }
}
