package com.siurbinfo.srs.service.cognito;

import com.siurbinfo.srs.dto.cognito.group.GroupResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListGroupsResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do CognitoGroupService.
 *
 * O CognitoIdentityProviderClient real (que fala com a AWS) e substituido por um
 * mock apos a construcao do service, de modo que nenhuma chamada de rede acontece.
 * Assim os testes exercitam a logica de mapeamento e de paginacao isoladamente.
 */
class CognitoGroupServiceTest {

    // issuer-uri no formato Cognito: o path (us-east-1_AbCdEf123) e o userPoolId
    // e o prefixo antes do "_" e a regiao.
    private static final String ISSUER_URI =
            "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_AbCdEf123";
    private static final String USER_POOL_ID = "us-east-1_AbCdEf123";

    private CognitoGroupService service;
    private CognitoIdentityProviderClient client;

    @BeforeEach
    void setUp() {
        service = new CognitoGroupService(ISSUER_URI);
        client = mock(CognitoIdentityProviderClient.class);
        // campo package-private: sobrescreve o client real construido no construtor
        service.client = client;
    }

    @Test
    void listGroup_returnsMappedGroupNames() {
        ListGroupsResponse response = ListGroupsResponse.builder()
                .groups(
                        GroupType.builder().groupName("admins").build(),
                        GroupType.builder().groupName("users").build())
                .nextToken(null)
                .build();
        when(client.listGroups(any(ListGroupsRequest.class))).thenReturn(response);

        List<GroupResponse> groups = service.listGroup();

        assertEquals(2, groups.size());
        assertEquals("admins", groups.get(0).groupName());
        assertEquals("users", groups.get(1).groupName());
    }

    @Test
    void listGroup_noGroups_returnsEmptyList() {
        when(client.listGroups(any(ListGroupsRequest.class)))
                .thenReturn(ListGroupsResponse.builder().build());

        assertTrue(service.listGroup().isEmpty());
    }

    @Test
    void listGroup_paginates_untilNextTokenIsNull() {
        ListGroupsResponse page1 = ListGroupsResponse.builder()
                .groups(GroupType.builder().groupName("g1").build())
                .nextToken("token-1")
                .build();
        ListGroupsResponse page2 = ListGroupsResponse.builder()
                .groups(GroupType.builder().groupName("g2").build())
                .nextToken(null)
                .build();
        when(client.listGroups(any(ListGroupsRequest.class))).thenReturn(page1, page2);

        List<GroupResponse> groups = service.listGroup();

        assertEquals(List.of("g1", "g2"),
                groups.stream().map(GroupResponse::groupName).toList());

        // duas paginas -> duas chamadas
        ArgumentCaptor<ListGroupsRequest> captor = ArgumentCaptor.forClass(ListGroupsRequest.class);
        verify(client, times(2)).listGroups(captor.capture());
        List<ListGroupsRequest> requests = captor.getAllValues();

        // userPoolId extraido do issuer-uri
        assertEquals(USER_POOL_ID, requests.get(0).userPoolId());
        // primeira pagina sem token; a segunda usa o token devolvido pela primeira
        assertNull(requests.get(0).nextToken());
        assertEquals("token-1", requests.get(1).nextToken());
    }
}
