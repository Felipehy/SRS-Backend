package com.siurbinfo.srs.service.cognito;

import com.siurbinfo.srs.dto.cognito.user.DeleteUserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponse;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.exception.AttributeIsEmptyException;
import com.siurbinfo.srs.exception.CognitoQueryException;
import com.siurbinfo.srs.exception.CognitoUnauthorizedException;
import com.siurbinfo.srs.mapper.user.UserMapper;
import com.siurbinfo.srs.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do CognitoUserService.
 *
 * O CognitoIdentityProviderClient real e substituido por um mock (via reflexao,
 * porque o campo e private final), evitando qualquer chamada a AWS. Cada teste
 * verifica o mapeamento das respostas, o tratamento de erros do Cognito e o
 * conteudo das requisicoes montadas pelo service.
 */
class CognitoUserServiceTest {

    private static final String ISSUER_URI =
            "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_AbCdEf123";
    private static final String USER_POOL_ID = "us-east-1_AbCdEf123";
    private static final String TEMP_PASSWORD = "Temp@1234";

    private CognitoUserService service;
    private CognitoIdentityProviderClient client;
    private UserMapper userMapper;
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userRepository = mock(UserRepository.class);
        service = new CognitoUserService(ISSUER_URI, TEMP_PASSWORD, userMapper, userRepository);
        client = mock(CognitoIdentityProviderClient.class);
        // campo private final -> injetado por reflexao
        ReflectionTestUtils.setField(service, "client", client);
    }

    // GetUser usa a sobrecarga que recebe um Consumer<GetUserRequest.Builder>.
    @SuppressWarnings("unchecked")
    private void stubGetUser(GetUserResponse response) {
        when(client.getUser(any(Consumer.class))).thenReturn(response);
    }

    private static Map<String, String> toMap(List<AttributeType> attributes) {
        return attributes.stream()
                .collect(Collectors.toMap(AttributeType::name, AttributeType::value));
    }

    // ---------- getName ----------

    @Test
    void getName_joinsGivenNameAndFamilyName() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(
                        AttributeType.builder().name("given_name").value("Fulano").build(),
                        AttributeType.builder().name("family_name").value("de Tal").build())
                .build());

        assertEquals("Fulano de Tal", service.getName("access-token"));
    }

    @Test
    void getName_onlyGivenName_returnsGivenName() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("given_name").value("Fulano").build())
                .build());

        assertEquals("Fulano", service.getName("access-token"));
    }

    @Test
    void getName_noNameAttributes_returnsNull() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("a@b.com").build())
                .build());

        assertNull(service.getName("access-token"));
    }

    // ---------- getEmail ----------

    @Test
    void getEmail_returnsEmailAttribute() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(
                        AttributeType.builder().name("given_name").value("Fulano").build(),
                        AttributeType.builder().name("email").value("fulano@empresa.com").build())
                .build());

        assertEquals("fulano@empresa.com", service.getEmail("access-token"));
    }

    @Test
    void getEmail_missingEmail_returnsNull() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("given_name").value("Fulano").build())
                .build());

        assertNull(service.getEmail("access-token"));
    }

    // ---------- getAttribute ----------

    @Test
    void getAttribute_returnsAttributeValue() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("a@b.com").build())
                .build());

        assertEquals("a@b.com", service.getAttribute("access-token", "email"));
    }

    @Test
    void getAttribute_missingAttribute_returnsNull() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("a@b.com").build())
                .build());

        assertNull(service.getAttribute("access-token", "phone_number"));
    }

    // ---------- tratamento de erros do GetUser ----------

    @Test
    @SuppressWarnings("unchecked")
    void getName_notAuthorized_throwsUnauthorized() {
        when(client.getUser(any(Consumer.class)))
                .thenThrow(NotAuthorizedException.builder().message("token invalido").build());

        assertThrows(CognitoUnauthorizedException.class, () -> service.getName("access-token"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAttribute_sdkException_throwsQueryException() {
        when(client.getUser(any(Consumer.class)))
                .thenThrow(CognitoIdentityProviderException.builder().message("indisponivel").build());

        assertThrows(CognitoQueryException.class, () -> service.getAttribute("access-token", "email"));
    }

    // ---------- createUser ----------

    @Test
    void createUser_sendsAttributesAndReturnsSuccessMessage() {
        // email com espacos ao redor deve ser "stripado"
        UserRequest dto = new UserRequest("Fulano", "de Tal", "  fulano@empresa.com  ", null);

        UserResponseMessage message = service.createUser(dto);

        assertEquals("Conta criada com sucesso", message.message());
        assertEquals("Fulano", message.given_name());

        ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor.forClass(AdminCreateUserRequest.class);
        verify(client).adminCreateUser(captor.capture());
        AdminCreateUserRequest request = captor.getValue();

        assertEquals(USER_POOL_ID, request.userPoolId());
        assertEquals("fulano@empresa.com", request.username());
        assertEquals(TEMP_PASSWORD, request.temporaryPassword());

        Map<String, String> attrs = toMap(request.userAttributes());
        assertEquals("Fulano", attrs.get("given_name"));
        assertEquals("de Tal", attrs.get("family_name"));
        assertEquals("fulano@empresa.com", attrs.get("email"));
        assertEquals("true", attrs.get("email_verified"));
        assertFalse(attrs.containsKey("phone_number")); // telefone nao informado
    }

    @Test
    void createUser_nationalPhone_isNormalizedToE164() {
        UserRequest dto = new UserRequest("Ana", "Silva", "ana@empresa.com", "11999998888");

        service.createUser(dto);

        ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor.forClass(AdminCreateUserRequest.class);
        verify(client).adminCreateUser(captor.capture());
        assertEquals("+5511999998888", toMap(captor.getValue().userAttributes()).get("phone_number"));
    }

    @Test
    void createUser_phoneWithCountryCodeAndSymbols_keepsE164() {
        UserRequest dto = new UserRequest("Ana", "Silva", "ana@empresa.com", "+55 (11) 99999-8888");

        service.createUser(dto);

        ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor.forClass(AdminCreateUserRequest.class);
        verify(client).adminCreateUser(captor.capture());
        assertEquals("+5511999998888", toMap(captor.getValue().userAttributes()).get("phone_number"));
    }

    @Test
    void createUser_blankPhone_isOmitted() {
        UserRequest dto = new UserRequest("Ana", "Silva", "ana@empresa.com", "   ");

        service.createUser(dto);

        ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor.forClass(AdminCreateUserRequest.class);
        verify(client).adminCreateUser(captor.capture());
        assertFalse(toMap(captor.getValue().userAttributes()).containsKey("phone_number"));
    }

    // ---------- listUsers ----------

    @Test
    void listUsers_mapsAttributesAndPaginates() {
        UserType u1 = UserType.builder()
                .username("user-1")
                .userStatus(UserStatusType.CONFIRMED)
                .attributes(
                        AttributeType.builder().name("given_name").value("Ana").build(),
                        AttributeType.builder().name("family_name").value("Silva").build(),
                        AttributeType.builder().name("email").value("ana@empresa.com").build())
                .build();
        UserType u2 = UserType.builder()
                .username("user-2")
                .userStatus(UserStatusType.FORCE_CHANGE_PASSWORD)
                .attributes(AttributeType.builder().name("email").value("bob@empresa.com").build())
                .build();

        ListUsersResponse page1 = ListUsersResponse.builder().users(u1).paginationToken("t1").build();
        ListUsersResponse page2 = ListUsersResponse.builder().users(u2).paginationToken(null).build();
        when(client.listUsers(any(ListUsersRequest.class))).thenReturn(page1, page2);

        List<UserResponse> users = service.listUsers();

        assertEquals(2, users.size());

        UserResponse first = users.get(0);
        assertEquals("user-1", first.username());
        assertEquals("Ana", first.given_name());
        assertEquals("Silva", first.family_name());
        assertEquals("ana@empresa.com", first.email());
        assertEquals("CONFIRMED", first.status());

        UserResponse second = users.get(1);
        assertEquals("user-2", second.username());
        assertNull(second.given_name()); // atributo ausente -> null
        assertEquals("bob@empresa.com", second.email());
        assertEquals("FORCE_CHANGE_PASSWORD", second.status());

        verify(client, times(2)).listUsers(any(ListUsersRequest.class));
    }

    // ---------- deleteUser ----------

    @Test
    void deleteUser_callsAdminDeleteWithEmailAsUsername() {
        service.deleteUser(new DeleteUserRequest("fulano@empresa.com"));

        ArgumentCaptor<AdminDeleteUserRequest> captor = ArgumentCaptor.forClass(AdminDeleteUserRequest.class);
        verify(client).adminDeleteUser(captor.capture());
        assertEquals(USER_POOL_ID, captor.getValue().userPoolId());
        assertEquals("fulano@empresa.com", captor.getValue().username());
    }

    // ---------- updateUserAtt ----------

    @Test
    void updateUserAtt_changedAttribute_updatesOnlyChangedField() {
        // estado atual no Cognito
        when(client.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(
                AdminGetUserResponse.builder()
                        .userAttributes(
                                AttributeType.builder().name("given_name").value("Antigo").build(),
                                AttributeType.builder().name("family_name").value("Silva").build())
                        .build());

        // given_name muda; family_name permanece igual
        UserResponseMessage message =
                service.updateUserAtt(new UserRequest("Novo", "Silva", "fulano@empresa.com", null));

        assertEquals("Usuario atualizado com sucesso", message.message());
        assertEquals("Novo", message.given_name());

        ArgumentCaptor<AdminUpdateUserAttributesRequest> captor =
                ArgumentCaptor.forClass(AdminUpdateUserAttributesRequest.class);
        verify(client).adminUpdateUserAttributes(captor.capture());
        Map<String, String> attrs = toMap(captor.getValue().userAttributes());

        assertEquals(1, attrs.size()); // apenas o campo alterado e enviado
        assertEquals("Novo", attrs.get("given_name"));
    }

    @Test
    void updateUserAtt_noChanges_throwsAndDoesNotUpdate() {
        when(client.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(
                AdminGetUserResponse.builder()
                        .userAttributes(
                                AttributeType.builder().name("given_name").value("Fulano").build(),
                                AttributeType.builder().name("family_name").value("Silva").build())
                        .build());

        assertThrows(AttributeIsEmptyException.class,
                () -> service.updateUserAtt(new UserRequest("Fulano", "Silva", "fulano@empresa.com", null)));

        verify(client, never()).adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class));
    }

    @Test
    void updateUserAtt_blankFields_throwsAttributeIsEmpty() {
        when(client.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(
                AdminGetUserResponse.builder()
                        .userAttributes(AttributeType.builder().name("given_name").value("Fulano").build())
                        .build());

        assertThrows(AttributeIsEmptyException.class,
                () -> service.updateUserAtt(new UserRequest("", "", "fulano@empresa.com", null)));
    }
}
