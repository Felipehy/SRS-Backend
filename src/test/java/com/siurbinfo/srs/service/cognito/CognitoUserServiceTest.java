package com.siurbinfo.srs.service.cognito;

import com.siurbinfo.srs.dto.cognito.group.GroupResponse;
import com.siurbinfo.srs.dto.cognito.user.DeleteUserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponse;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.dto.department.DepartmentResponseDTO;
import com.siurbinfo.srs.dto.reservation.ReservationsDTO;
import com.siurbinfo.srs.dto.user.ColorsRequestDTO;
import com.siurbinfo.srs.dto.user.ListReservationsUserDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import com.siurbinfo.srs.entity.department.DepartmentEntity;
import com.siurbinfo.srs.entity.room.RoomEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.exception.AttributeIsEmptyException;
import com.siurbinfo.srs.exception.CognitoQueryException;
import com.siurbinfo.srs.exception.CognitoUnauthorizedException;
import com.siurbinfo.srs.exception.DepartmentIdNotFoundedException;
import com.siurbinfo.srs.exception.UserIdNotFoundedException;
import com.siurbinfo.srs.mapper.department.DepartmentMapper;
import com.siurbinfo.srs.mapper.user.UserMapper;
import com.siurbinfo.srs.repository.user.ReservationsProjection;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.group.CognitoGroupServiceImpl;
import com.siurbinfo.srs.service.cognito.user.CognitoUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CognitoUserServiceTest {

    private static final String ISSUER_URI =
            "https://cognito-idp.us-east-1.amazonaws.com/us-east-1_AbCdEf123";
    private static final String USER_POOL_ID = "us-east-1_AbCdEf123";
    private static final String TEMP_PASSWORD = "Temp@1234";

    private static final Jwt JWT = Jwt.withTokenValue("access-token")
            .header("alg", "none")
            .claim("sub", "usuario")
            .build();

    private CognitoUserServiceImpl service;
    private CognitoIdentityProviderClient client;
    private UserMapper userMapper;
    private UserRepository userRepository;
    private CognitoGroupServiceImpl cognitoGroupService;
    private DepartmentMapper departmentMapper;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userRepository = mock(UserRepository.class);
        cognitoGroupService = mock(CognitoGroupServiceImpl.class);
        departmentMapper = mock(DepartmentMapper.class);
        service = new CognitoUserServiceImpl(ISSUER_URI, TEMP_PASSWORD, userMapper, userRepository,
                cognitoGroupService, departmentMapper);
        when(userMapper.toEntity(any(UserRequest.class))).thenAnswer(invocation -> new UserEntity());
        client = mock(CognitoIdentityProviderClient.class);
        ReflectionTestUtils.setField(service, "client", client);
    }

    @SuppressWarnings("unchecked")
    private void stubGetUser(GetUserResponse response) {
        when(client.getUser(any(Consumer.class))).thenReturn(response);
    }

    private static Map<String, String> toMap(List<AttributeType> attributes) {
        return attributes.stream()
                .collect(Collectors.toMap(AttributeType::name, AttributeType::value));
    }

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

    private static UserEntity linhaDoUsuario() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("fulano@empresa.com");
        user.setGivenName("Fulano");
        user.setFamilyName("de Tal");
        return user;
    }

    @Test
    void getUser_nullJwt_throwsSecurityException() {
        assertThrows(SecurityException.class, () -> service.getUser(null));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(cognitoGroupService);
    }

    @Test
    void getUser_returnsNamesColorsAndPreferencesFromDatabase() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("fulano@empresa.com").build())
                .build());
        UserEntity user = linhaDoUsuario();
        user.setColorAuditorium("#111111");
        user.setColorFleetVehicles("#222222");
        user.setColorMeetingRoom("#333333");
        user.setIsHorizontalNavbar(true);
        user.setIsFirstLogin(false);
        DepartmentEntity department = new DepartmentEntity();
        department.setId(42L);
        user.setDepartment(department);
        when(userRepository.getUserByEmail("fulano@empresa.com")).thenReturn(user);

        UserResponseDTO dto = service.getUser(JWT);

        assertEquals(1L, dto.id());
        assertEquals("fulano@empresa.com", dto.email());
        assertEquals("Fulano", dto.givenName());
        assertEquals("de Tal", dto.familyName());
        assertEquals("#111111", dto.colors().get("colorAuditorium"));
        assertEquals("#222222", dto.colors().get("colorFleetVehicle"));
        assertEquals("#333333", dto.colors().get("colorMeetingRoom"));
        assertTrue(dto.isHorizontalNavbar());
        assertEquals(42L, dto.idDepartment());
        assertFalse(dto.isFirstLogin());
    }

    @Test
    void getUser_returnsGroupsOfTheAuthenticatedUser() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("fulano@empresa.com").build())
                .build());
        when(userRepository.getUserByEmail("fulano@empresa.com")).thenReturn(linhaDoUsuario());
        when(cognitoGroupService.listGroupsForUser(anyString()))
                .thenReturn(List.of(new GroupResponse("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos")));

        UserResponseDTO dto = service.getUser(JWT);

        assertEquals(List.of(new GroupResponse("Frota", "siurb-frota", "#ff0000", "Reserva de veiculos")),
                dto.groups());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(cognitoGroupService).listGroupsForUser(captor.capture());
        assertEquals("fulano@empresa.com", captor.getValue());
    }

    @Test
    void getUser_userWithoutGroups_returnsEmptyList() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("fulano@empresa.com").build())
                .build());
        when(userRepository.getUserByEmail("fulano@empresa.com")).thenReturn(linhaDoUsuario());
        when(cognitoGroupService.listGroupsForUser(anyString())).thenReturn(List.of());

        assertTrue(service.getUser(JWT).groups().isEmpty());
    }

    @Test
    void getUser_userWithoutDepartment_returnsNullIdDepartment() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("fulano@empresa.com").build())
                .build());
        when(userRepository.getUserByEmail("fulano@empresa.com")).thenReturn(linhaDoUsuario());

        assertNull(service.getUser(JWT).idDepartment());
    }

    @Test
    void getUser_userWithoutRowInDatabase_returnsNullColors() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("fulano@empresa.com").build())
                .build());
        when(userRepository.getUserByEmail("fulano@empresa.com")).thenReturn(null);
        when(cognitoGroupService.listGroupsForUser(anyString())).thenReturn(List.of());

        UserResponseDTO dto = service.getUser(JWT);

        assertNull(dto.id());
        assertEquals("fulano@empresa.com", dto.email());
        assertTrue(dto.colors().containsKey("colorAuditorium"));
        assertNull(dto.colors().get("colorAuditorium"));
        assertNull(dto.colors().get("colorFleetVehicle"));
        assertNull(dto.colors().get("colorMeetingRoom"));
        assertNull(dto.idDepartment());
        assertTrue(dto.groups().isEmpty());
    }

    @Test
    void getUser_userNeverSetColors_returnsNullValues() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("fulano@empresa.com").build())
                .build());
        when(userRepository.getUserByEmail("fulano@empresa.com")).thenReturn(linhaDoUsuario());

        UserResponseDTO dto = service.getUser(JWT);

        assertNull(dto.colors().get("colorAuditorium"));
        assertNull(dto.colors().get("colorFleetVehicle"));
        assertNull(dto.colors().get("colorMeetingRoom"));
    }

    @Test
    void finishTutorial_nullJwt_throwsAndDoesNotTouchDatabase() {
        assertThrows(SecurityException.class, () -> service.finishTutorial(null));
        verifyNoInteractions(userRepository);
    }

    @Test
    void finishTutorial_marksFirstLoginAsFalseForTokenOwner() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("fulano@empresa.com").build())
                .build());

        service.finishTutorial(JWT);

        verify(userRepository).setFirstLogin(false, "fulano@empresa.com");
    }

    @Test
    void setColors_nullJwt_throwsAndDoesNotTouchDatabase() {
        ColorsRequestDTO dto = new ColorsRequestDTO("#111111", "#222222", "#333333");

        assertThrows(SecurityException.class, () -> service.setColors(null, dto));
        verifyNoInteractions(userRepository);
    }

    @Test
    void setColors_savesColorsForEmailOfTokenOwner() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value("fulano@empresa.com").build())
                .build());

        service.setColors(JWT, new ColorsRequestDTO("#111111", "#222222", "#333333"));

        verify(userRepository).setColors("fulano@empresa.com", "#111111", "#222222", "#333333");
    }

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

    @Test
    void createUser_sendsAttributesAndReturnsSuccessMessage() {
        UserRequest dto = new UserRequest("Fulano", "de Tal", "  fulano@empresa.com  ", null, null);

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
        assertFalse(attrs.containsKey("phone_number"));
    }

    @Test
    void createUser_nationalPhone_isNormalizedToE164() {
        UserRequest dto = new UserRequest("Ana", "Silva", "ana@empresa.com", null, "11999998888");

        service.createUser(dto);

        ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor.forClass(AdminCreateUserRequest.class);
        verify(client).adminCreateUser(captor.capture());
        assertEquals("+5511999998888", toMap(captor.getValue().userAttributes()).get("phone_number"));
    }

    @Test
    void createUser_phoneWithCountryCodeAndSymbols_keepsE164() {
        UserRequest dto = new UserRequest("Ana", "Silva", "ana@empresa.com", null, "+55 (11) 99999-8888");

        service.createUser(dto);

        ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor.forClass(AdminCreateUserRequest.class);
        verify(client).adminCreateUser(captor.capture());
        assertEquals("+5511999998888", toMap(captor.getValue().userAttributes()).get("phone_number"));
    }

    @Test
    void createUser_blankPhone_isOmitted() {
        UserRequest dto = new UserRequest("Ana", "Silva", "ana@empresa.com", null, "   ");

        service.createUser(dto);

        ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor.forClass(AdminCreateUserRequest.class);
        verify(client).adminCreateUser(captor.capture());
        assertFalse(toMap(captor.getValue().userAttributes()).containsKey("phone_number"));
    }

    @Test
    void setDepartment_gravaParaOEmailInformado() {
        service.setDepartment(7L, "fulano@empresa.com");

        verify(userRepository).setDepartment(7L, "fulano@empresa.com");
        verifyNoInteractions(client);
    }

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
        when(userRepository.getIdDepartmentByEmail("ana@empresa.com")).thenReturn(11L);
        when(userRepository.getIdDepartmentByEmail("bob@empresa.com")).thenReturn(null);

        List<UserResponse> users = service.listUsers();

        assertEquals(2, users.size());

        UserResponse first = users.get(0);
        assertEquals("user-1", first.username());
        assertEquals("Ana", first.given_name());
        assertEquals("Silva", first.family_name());
        assertEquals("ana@empresa.com", first.email());
        assertEquals("CONFIRMED", first.status());
        assertEquals(11L, first.idDepartment());

        UserResponse second = users.get(1);
        assertEquals("user-2", second.username());
        assertNull(second.given_name());
        assertEquals("bob@empresa.com", second.email());
        assertEquals("FORCE_CHANGE_PASSWORD", second.status());
        assertNull(second.idDepartment());

        verify(client, times(2)).listUsers(any(ListUsersRequest.class));
    }

    @Test
    void listUsers_consultaODepartamentoUmaVezPorUsuario() {
        when(client.listUsers(any(ListUsersRequest.class))).thenReturn(
                ListUsersResponse.builder()
                        .users(
                                UserType.builder().username("u1")
                                        .attributes(AttributeType.builder().name("email").value("a@x.com").build())
                                        .build(),
                                UserType.builder().username("u2")
                                        .attributes(AttributeType.builder().name("email").value("b@x.com").build())
                                        .build())
                        .paginationToken(null)
                        .build());

        service.listUsers();

        verify(userRepository).getIdDepartmentByEmail("a@x.com");
        verify(userRepository).getIdDepartmentByEmail("b@x.com");
    }

    @Test
    void deleteUser_callsAdminDeleteWithEmailAsUsername() {
        service.deleteUser(new DeleteUserRequest("fulano@empresa.com"));

        ArgumentCaptor<AdminDeleteUserRequest> captor = ArgumentCaptor.forClass(AdminDeleteUserRequest.class);
        verify(client).adminDeleteUser(captor.capture());
        assertEquals(USER_POOL_ID, captor.getValue().userPoolId());
        assertEquals("fulano@empresa.com", captor.getValue().username());
    }

    @Test
    void deleteUser_devolveOIdLidoAntesDaExclusaoLogica() {
        when(userRepository.getIdByEmail("fulano@empresa.com")).thenReturn(42L);

        UserResponseMessage message = service.deleteUser(new DeleteUserRequest("fulano@empresa.com"));

        assertEquals(42L, message.id());
        InOrder ordem = inOrder(userRepository);
        ordem.verify(userRepository).getIdByEmail("fulano@empresa.com");
        ordem.verify(userRepository).deleteByEmail(eq("fulano@empresa.com"), any(Instant.class));
    }

    @Test
    void resetPassword_defineASenhaTemporariaSemSerPermanente() {
        when(userRepository.getIdByEmail("fulano@empresa.com")).thenReturn(42L);

        UserResponseMessage message = service.resetPassword("fulano@empresa.com", JWT);

        ArgumentCaptor<AdminSetUserPasswordRequest> captor = ArgumentCaptor.forClass(AdminSetUserPasswordRequest.class);
        verify(client).adminSetUserPassword(captor.capture());
        AdminSetUserPasswordRequest request = captor.getValue();
        assertEquals(USER_POOL_ID, request.userPoolId());
        assertEquals("fulano@empresa.com", request.username());
        assertEquals(TEMP_PASSWORD, request.password());
        assertFalse(request.permanent());

        assertEquals(42L, message.id());
    }

    @Test
    void resetMFA_apagaOTokenDeSoftwareDoUsuarioNoPool() {
        service.resetMFA("fulano@empresa.com");

        ArgumentCaptor<AdminDeleteSoftwareTokenRequest> captor =
                ArgumentCaptor.forClass(AdminDeleteSoftwareTokenRequest.class);
        verify(client).adminDeleteSoftwareToken(captor.capture());
        assertEquals(USER_POOL_ID, captor.getValue().userPoolId());
        assertEquals("fulano@empresa.com", captor.getValue().username());
        verifyNoInteractions(userRepository);
    }

    @Test
    void updateUserAtt_changedAttribute_updatesOnlyChangedField() {
        when(client.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(
                AdminGetUserResponse.builder()
                        .userAttributes(
                                AttributeType.builder().name("given_name").value("Antigo").build(),
                                AttributeType.builder().name("family_name").value("Silva").build())
                        .build());

        UserResponseMessage message =
                service.updateUserAtt(new UserRequest("Novo", "Silva", "fulano@empresa.com", null, null));

        assertEquals("Usuario atualizado com sucesso", message.message());
        assertEquals("Novo", message.given_name());

        ArgumentCaptor<AdminUpdateUserAttributesRequest> captor =
                ArgumentCaptor.forClass(AdminUpdateUserAttributesRequest.class);
        verify(client).adminUpdateUserAttributes(captor.capture());
        Map<String, String> attrs = toMap(captor.getValue().userAttributes());

        assertEquals(1, attrs.size());
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
                () -> service.updateUserAtt(new UserRequest("Fulano", "Silva", "fulano@empresa.com", null, null)));

        verify(client, never()).adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class));
    }

    @Test
    void updateUserAtt_blankFields_throwsAttributeIsEmpty() {
        when(client.adminGetUser(any(AdminGetUserRequest.class))).thenReturn(
                AdminGetUserResponse.builder()
                        .userAttributes(AttributeType.builder().name("given_name").value("Fulano").build())
                        .build());

        assertThrows(AttributeIsEmptyException.class,
                () -> service.updateUserAtt(new UserRequest("", "", "fulano@empresa.com", null, null)));
    }

    private static final String EMAIL = "fulano@empresa.com";
    private static final Long ID_USUARIO = 7L;
    private static final Long ID_DEPARTAMENTO = 42L;

    private void stubEmailDoToken() {
        stubGetUser(GetUserResponse.builder()
                .userAttributes(AttributeType.builder().name("email").value(EMAIL).build())
                .build());
    }

    private static UserEntity usuarioCom(DepartmentEntity department) {
        UserEntity user = new UserEntity();
        user.setId(ID_USUARIO);
        user.setEmail(EMAIL);
        user.setDepartment(department);
        return user;
    }

    private static DepartmentEntity departamento() {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setId(ID_DEPARTAMENTO);
        entity.setName("Tecnologia");
        RoomEntity room = new RoomEntity();
        room.setId(3L);
        room.setFloor("3 ANDAR");
        entity.setRoom(room);
        return entity;
    }

    @Test
    void getDepartmentFromUser_semJwt_lancaSecurityException() {
        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.getDepartmentFromUser(ID_USUARIO, null));

        assertEquals("Is necessary a Json web token", ex.getMessage());
        verifyNoInteractions(userRepository, departmentMapper);
    }

    @Test
    void getDepartmentFromUser_idDeOutroUsuario_lancaSecurityException() {
        stubEmailDoToken();
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(99L);

        assertThrows(SecurityException.class, () -> service.getDepartmentFromUser(ID_USUARIO, JWT));

        verify(userRepository, never()).findById(any());
        verifyNoInteractions(departmentMapper);
    }

    @Test
    void getDepartmentFromUser_doProprioUsuario_retornaODepartamentoVinculado() {
        DepartmentEntity entity = departamento();
        DepartmentResponseDTO esperado = new DepartmentResponseDTO("Tecnologia", "3 ANDAR", 3L, ID_DEPARTAMENTO);
        stubEmailDoToken();
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_USUARIO);
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuarioCom(entity)));
        when(departmentMapper.toResponseDTO(entity)).thenReturn(esperado);

        assertEquals(esperado, service.getDepartmentFromUser(ID_USUARIO, JWT));
    }

    @Test
    void getDepartmentFromUser_usuarioSemDepartamento_lancaDepartmentIdNotFounded() {
        stubEmailDoToken();
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_USUARIO);
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.of(usuarioCom(null)));

        DepartmentIdNotFoundedException ex = assertThrows(DepartmentIdNotFoundedException.class,
                () -> service.getDepartmentFromUser(ID_USUARIO, JWT));

        assertEquals("Usuário não possui departamento vinculado", ex.getMessage());
        verifyNoInteractions(departmentMapper);
    }

    @Test
    void getDepartmentFromUser_usuarioInexistente_lancaUserIdNotFounded() {
        stubEmailDoToken();
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_USUARIO);
        when(userRepository.findById(ID_USUARIO)).thenReturn(Optional.empty());

        assertThrows(UserIdNotFoundedException.class,
                () -> service.getDepartmentFromUser(ID_USUARIO, JWT));
    }

    @Test
    void getDepartmentFromUser_emailSemUsuarioNoBanco_estouraNullPointer() {
        stubEmailDoToken();
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> service.getDepartmentFromUser(ID_USUARIO, JWT));
    }

    private static final LocalDate DIA = LocalDate.of(2026, 8, 1);

    private static final List<String> TODOS_OS_TIPOS = List.of("FLEET_VEHICLE", "AUDITORIUM", "MEETING_ROOM");

    private static ReservationsProjection projecao(Long id, String tipo, LocalTime inicio, LocalTime fim, long total) {
        return new ReservationsProjection(id, tipo, "APROVADA", DIA, inicio, fim, "Fulano de Tal", ID_USUARIO, total);
    }

    @Test
    void getReservationsByUser_jwtNulo_lancaSecurityException() {
        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.getReservationsByUser(null, null, 0L, null));

        assertEquals("Is necessary a json web token(jwt)", ex.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(client);
    }

    @Test
    void getReservationsByUser_montaPainelComTotalELista() {
        stubEmailDoToken();
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_USUARIO);
        when(userRepository.getReservationsByIdUser(ID_USUARIO, null, 0L, TODOS_OS_TIPOS)).thenReturn(List.of(
                projecao(1L, "AUDITORIUM", LocalTime.of(9, 0), LocalTime.of(11, 0), 12L),
                projecao(2L, "FLEET_VEHICLE", LocalTime.of(8, 0), null, 12L)));

        ReservationsDTO painel = service.getReservationsByUser(JWT, null, 0L, null);

        assertEquals(12L, painel.numReservations());
        assertEquals(List.of(
                new ListReservationsUserDTO(1L, "AUDITORIUM", "APROVADA", DIA,
                        LocalTime.of(9, 0), LocalTime.of(11, 0), "Fulano de Tal", ID_USUARIO),
                new ListReservationsUserDTO(2L, "FLEET_VEHICLE", "APROVADA", DIA,
                        LocalTime.of(8, 0), null, "Fulano de Tal", ID_USUARIO)),
                painel.reservations());
    }

    @Test
    void getReservationsByUser_paginaViraOffsetDeDezEmDezEDataVaiPraQuery() {
        stubEmailDoToken();
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_USUARIO);
        when(userRepository.getReservationsByIdUser(ID_USUARIO, DIA, 20L, List.of("MEETING_ROOM")))
                .thenReturn(List.of(projecao(21L, "MEETING_ROOM", LocalTime.of(14, 0), LocalTime.of(15, 0), 21L)));

        service.getReservationsByUser(JWT, DIA, 2L, List.of("MEETING_ROOM"));

        verify(userRepository).getReservationsByIdUser(ID_USUARIO, DIA, 20L, List.of("MEETING_ROOM"));
    }

    @Test
    void getReservationsByUser_listaDeTiposVazia_buscaOsTres() {
        stubEmailDoToken();
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_USUARIO);

        service.getReservationsByUser(JWT, null, 0L, List.of());

        verify(userRepository).getReservationsByIdUser(ID_USUARIO, null, 0L, TODOS_OS_TIPOS);
    }

    @Test
    void getReservationsByUser_semReservas_devolvePainelZerado() {
        stubEmailDoToken();
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_USUARIO);
        when(userRepository.getReservationsByIdUser(ID_USUARIO, null, 0L, TODOS_OS_TIPOS)).thenReturn(List.of());

        ReservationsDTO painel = service.getReservationsByUser(JWT, null, 0L, null);

        assertEquals(0L, painel.numReservations());
        assertTrue(painel.reservations().isEmpty());
    }

    @Test
    void getReservationsByUser_emailSemUsuarioNoBanco_devolvePainelZerado() {
        stubEmailDoToken();
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(null);
        when(userRepository.getReservationsByIdUser(null, null, 0L, TODOS_OS_TIPOS)).thenReturn(List.of());

        ReservationsDTO painel = service.getReservationsByUser(JWT, null, 0L, null);

        assertEquals(0L, painel.numReservations());
        assertTrue(painel.reservations().isEmpty());
    }
}
