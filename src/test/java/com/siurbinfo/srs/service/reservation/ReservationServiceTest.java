package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.ReservationsDTO;
import com.siurbinfo.srs.dto.user.ListReservationsUserDTO;
import com.siurbinfo.srs.exception.InvalidTimeException;
import com.siurbinfo.srs.repository.reservation.ReservationRepository;
import com.siurbinfo.srs.repository.user.ReservationsProjection;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationServiceTest {

    private static final String EMAIL = "fulano@empresa.com";
    private static final Long ID_USUARIO = 7L;
    private static final LocalDate INICIO = LocalDate.of(2026, 8, 1);
    private static final LocalDate FIM = LocalDate.of(2026, 9, 1);

    private static final Jwt JWT = Jwt.withTokenValue("access-token")
            .header("alg", "none")
            .claim("sub", "usuario")
            .build();

    private ReservationRepository repository;
    private CognitoUserService userService;
    private UserRepository userRepository;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ReservationRepository.class);
        userService = mock(CognitoUserService.class);
        userRepository = mock(UserRepository.class);
        service = new ReservationService(repository, userService, userRepository);
    }

    private void stubUsuarioDoToken() {
        when(userService.getEmail("access-token")).thenReturn(EMAIL);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_USUARIO);
    }

    private static ReservationsProjection projecao(Long id, String tipo, LocalTime inicio, LocalTime fim, long total) {
        return new ReservationsProjection(id, tipo, "APROVADA", INICIO, inicio, fim, "Fulano de Tal", ID_USUARIO, total);
    }

    @Test
    void getAllReservations_jwtNulo_lancaSecurityException() {
        SecurityException ex = assertThrows(SecurityException.class,
                () -> service.getAllReservations(INICIO, FIM, null));

        assertEquals("Is necessary a json web token(jwt)", ex.getMessage());
        verifyNoInteractions(userService, userRepository, repository);
    }

    @Test
    void getAllReservations_semInicio_lancaInvalidTimeException() {
        InvalidTimeException ex = assertThrows(InvalidTimeException.class,
                () -> service.getAllReservations(null, FIM, JWT));

        assertEquals("É necessário o tempo inicial", ex.getMessage());
        verifyNoInteractions(userService, userRepository, repository);
    }

    @Test
    void getAllReservations_consultaComOUsuarioDoToken() {
        stubUsuarioDoToken();
        when(repository.getAllReservations(ID_USUARIO, INICIO, FIM)).thenReturn(List.of());

        service.getAllReservations(INICIO, FIM, JWT);

        verify(repository).getAllReservations(ID_USUARIO, INICIO, FIM);
    }

    @Test
    void getAllReservations_montaListaComTotal() {
        stubUsuarioDoToken();
        when(repository.getAllReservations(ID_USUARIO, INICIO, FIM)).thenReturn(List.of(
                projecao(1L, "AUDITORIUM", LocalTime.of(9, 0), LocalTime.of(11, 0), 2L),
                projecao(5L, "FLEET_VEHICLE", LocalTime.of(8, 0), null, 2L)));

        ReservationsDTO dto = service.getAllReservations(INICIO, FIM, JWT);

        assertEquals(2L, dto.numReservations());
        assertEquals(List.of(
                new ListReservationsUserDTO(1L, "AUDITORIUM", "APROVADA", INICIO,
                        LocalTime.of(9, 0), LocalTime.of(11, 0), "Fulano de Tal", ID_USUARIO),
                new ListReservationsUserDTO(5L, "FLEET_VEHICLE", "APROVADA", INICIO,
                        LocalTime.of(8, 0), null, "Fulano de Tal", ID_USUARIO)),
                dto.reservations());
    }

    @Test
    void getAllReservations_semReservasNoIntervalo_devolveListaVazia() {
        stubUsuarioDoToken();
        when(repository.getAllReservations(ID_USUARIO, INICIO, FIM)).thenReturn(List.of());

        ReservationsDTO dto = service.getAllReservations(INICIO, FIM, JWT);

        assertEquals(0L, dto.numReservations());
        assertTrue(dto.reservations().isEmpty());
    }

    @Test
    void getAllReservations_semFim_repassaNullPraQuery() {
        stubUsuarioDoToken();
        when(repository.getAllReservations(ID_USUARIO, INICIO, null)).thenReturn(List.of());

        service.getAllReservations(INICIO, null, JWT);

        verify(repository).getAllReservations(ID_USUARIO, INICIO, null);
    }
}
