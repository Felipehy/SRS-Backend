package com.siurbinfo.srs.service.cognito.user;

import com.siurbinfo.srs.dto.cognito.group.GroupResponse;
import com.siurbinfo.srs.dto.department.DepartmentResponseDTO;
import com.siurbinfo.srs.dto.reservation.ReservationsDTO;
import com.siurbinfo.srs.dto.user.ListReservationsUserDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import com.siurbinfo.srs.entity.department.DepartmentEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.exception.DepartmentIdNotFoundedException;
import com.siurbinfo.srs.mapper.department.DepartmentMapper;
import com.siurbinfo.srs.repository.user.ReservationsProjection;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.group.CognitoGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DevCognitoUserServiceTest {

    private static final String EMAIL = "dev@prefeitura.sp.gov.br";

    private static final Jwt JWT = Jwt.withTokenValue("local-fake-token")
            .header("alg", "none")
            .claim("sub", EMAIL)
            .build();

    private DevCognitoUserService service;
    private UserRepository userRepository;
    private CognitoGroupService cognitoGroupService;
    private DepartmentMapper departmentMapper;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        cognitoGroupService = mock(CognitoGroupService.class);
        departmentMapper = mock(DepartmentMapper.class);
        service = new DevCognitoUserService(userRepository, cognitoGroupService, departmentMapper,
                EMAIL, "Dev", "Local");
    }

    @Test
    void getUser_jwtNulo_lancaSecurityException() {
        assertThrows(SecurityException.class, () -> service.getUser(null));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(cognitoGroupService);
    }

    @Test
    void getUser_devolveDadosDeTblUserEGruposDoUsuarioFake() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setEmail(EMAIL);
        user.setGivenName("Dev");
        user.setFamilyName("Local");
        user.setColorAuditorium("#111111");
        user.setColorFleetVehicles("#222222");
        user.setColorMeetingRoom("#333333");
        user.setIsHorizontalNavbar(true);
        user.setIsFirstLogin(true);
        DepartmentEntity department = new DepartmentEntity();
        department.setId(42L);
        user.setDepartment(department);
        when(userRepository.getUserByEmail(EMAIL)).thenReturn(user);
        when(cognitoGroupService.listGroupsForUser(anyString()))
                .thenReturn(List.of(new GroupResponse("Administrador", "siurb-administrador", "#ffffff", "Tudo")));

        UserResponseDTO dto = service.getUser(JWT);

        assertEquals(7L, dto.id());
        assertEquals("Dev", dto.givenName());
        assertEquals("Local", dto.familyName());
        assertEquals("#111111", dto.colors().get("colorAuditorium"));
        assertEquals("#222222", dto.colors().get("colorFleetVehicle"));
        assertEquals("#333333", dto.colors().get("colorMeetingRoom"));
        assertTrue(dto.isHorizontalNavbar());
        assertEquals(42L, dto.idDepartment());
        assertTrue(dto.isFirstLogin());
        assertEquals(List.of(new GroupResponse("Administrador", "siurb-administrador", "#ffffff", "Tudo")),
                dto.groups());
    }

    @Test
    void getUser_usuarioSemDepartamento_idDepartmentNulo() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setEmail(EMAIL);
        when(userRepository.getUserByEmail(EMAIL)).thenReturn(user);

        assertNull(service.getUser(JWT).idDepartment());
    }

    @Test
    void getUser_consultaOsGruposSempreComOUsuarioFake() {
        when(cognitoGroupService.listGroupsForUser(anyString())).thenReturn(List.of());

        service.getUser(JWT);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(cognitoGroupService).listGroupsForUser(captor.capture());
        assertEquals(EMAIL, captor.getValue());
    }

    @Test
    void finishTutorial_jwtNulo_lancaENaoMexeNoBanco() {
        assertThrows(SecurityException.class, () -> service.finishTutorial(null));
        verifyNoInteractions(userRepository);
    }

    @Test
    void finishTutorial_marcaFirstLoginFalsoDoUsuarioFake() {
        service.finishTutorial(JWT);

        verify(userRepository).setFirstLogin(false, EMAIL);
    }

    @Test
    void setDepartment_usaOEmailInformadoENaoODoUsuarioFake() {
        service.setDepartment(7L, "outro@prefeitura.sp.gov.br");

        verify(userRepository).setDepartment(7L, "outro@prefeitura.sp.gov.br");
    }

    @Test
    void resetDeSenhaEDeMfa_naoSuportadosNoDev() {
        assertThrows(UnsupportedOperationException.class,
                () -> service.resetPassword("outro@prefeitura.sp.gov.br", JWT));
        assertThrows(UnsupportedOperationException.class,
                () -> service.resetMFA("outro@prefeitura.sp.gov.br"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void getUser_semLinhaEmTblUser_naoQuebraECoresFicamNulas() {
        when(userRepository.getUserByEmail(EMAIL)).thenReturn(null);
        when(cognitoGroupService.listGroupsForUser(anyString())).thenReturn(List.of());

        UserResponseDTO dto = service.getUser(JWT);

        assertEquals(EMAIL, dto.email());
        assertTrue(dto.colors().containsKey("colorAuditorium"));
        assertNull(dto.colors().get("colorAuditorium"));
        assertNull(dto.colors().get("colorFleetVehicle"));
        assertNull(dto.colors().get("colorMeetingRoom"));
        assertTrue(dto.groups().isEmpty());
    }


    @Test
    void getDepartmentFromUser_semJwt_lancaSecurityException() {
        assertThrows(SecurityException.class, () -> service.getDepartmentFromUser(7L, null));
        verifyNoInteractions(userRepository, departmentMapper);
    }

    @Test
    void getDepartmentFromUser_idDeOutroUsuario_lancaSecurityException() {
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);

        assertThrows(SecurityException.class, () -> service.getDepartmentFromUser(99L, JWT));

        verify(userRepository, never()).findById(any());
        verifyNoInteractions(departmentMapper);
    }

    @Test
    void getDepartmentFromUser_doUsuarioFake_retornaODepartamentoVinculado() {
        DepartmentEntity department = new DepartmentEntity();
        department.setId(42L);
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setDepartment(department);
        DepartmentResponseDTO esperado = new DepartmentResponseDTO("Tecnologia", "3 ANDAR", 3L, 42L);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(departmentMapper.toResponseDTO(department)).thenReturn(esperado);

        assertEquals(esperado, service.getDepartmentFromUser(7L, JWT));
    }

    @Test
    void getDepartmentFromUser_usuarioSemDepartamento_lancaDepartmentIdNotFounded() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThrows(DepartmentIdNotFoundedException.class,
                () -> service.getDepartmentFromUser(7L, JWT));

        verifyNoInteractions(departmentMapper);
    }

    private static final LocalDate DIA = LocalDate.of(2026, 8, 1);

    @Test
    void getReservationsByUser_jwtNulo_lancaSecurityException() {
        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.getReservationsByUser(null, null, 0L, null));

        assertEquals("Is necessary a json web token(jwt)", ex.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    void getReservationsByUser_usaOUsuarioFakeEMontaPainel() {
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);
        when(userRepository.getReservationsByIdUser(7L, DIA, 10L, List.of("MEETING_ROOM"))).thenReturn(List.of(
                new ReservationsProjection(3L, "MEETING_ROOM", "ENVIADA_PARA_ANALISE", DIA,
                        LocalTime.of(14, 0), LocalTime.of(15, 0), "Dev Local", 7L, 11L)));

        ReservationsDTO painel = service.getReservationsByUser(JWT, DIA, 1L, List.of("MEETING_ROOM"));

        assertEquals(11L, painel.numReservations());
        assertEquals(List.of(new ListReservationsUserDTO(3L, "MEETING_ROOM", "ENVIADA_PARA_ANALISE", DIA,
                LocalTime.of(14, 0), LocalTime.of(15, 0), "Dev Local", 7L)), painel.reservations());
    }

    @Test
    void getReservationsByUser_semReservas_devolvePainelZerado() {
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);
        when(userRepository.getReservationsByIdUser(7L, null, 0L, List.of("FLEET_VEHICLE", "AUDITORIUM", "MEETING_ROOM"))).thenReturn(List.of());

        ReservationsDTO painel = service.getReservationsByUser(JWT, null, 0L, null);

        assertEquals(0L, painel.numReservations());
        assertTrue(painel.reservations().isEmpty());
    }
}
