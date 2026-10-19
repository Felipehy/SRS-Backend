package com.siurbinfo.srs.service.reservation.validators;

import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.ReservationWasCancell;
import com.siurbinfo.srs.repository.reservation.ReservationMeetingRoomRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MeetingRoomValidatorTest {

    private static final Long ID_RESERVA = 1L;
    private static final String EMAIL = "solicitante@teste.com";
    private static final Long ID_DONO = 10L;
    private static final Long ID_OUTRO = 99L;

    private static final List<String> SEM_PRIVILEGIO = List.of("siurb-usuario");
    private static final List<String> DIVISAO_ADMINISTRATIVA = List.of("siurb-divisao-administrativa");

    private ReservationMeetingRoomRepository repository;
    private UserRepository userRepository;
    private MeetingRoomValidator validator;

    @BeforeEach
    void setUp() {
        repository = mock(ReservationMeetingRoomRepository.class);
        userRepository = mock(UserRepository.class);
        validator = new MeetingRoomValidator(new ReservationValidator<>(), repository, userRepository);
    }

    private MeetingRoomEntity reserva(ReserveStatus status, Long idDono) {
        MeetingRoomEntity entity = new MeetingRoomEntity();
        entity.setId(ID_RESERVA);
        entity.setStatus(status);
        if (idDono != null) {
            UserEntity dono = new UserEntity();
            dono.setId(idDono);
            entity.setUser(dono);
        }
        return entity;
    }

    @Test
    void validatorCancel_reservaInexistente_lancaReservationIdIsNotFounded() {
        when(repository.findById(ID_RESERVA)).thenReturn(Optional.empty());

        ReservationIdIsNotFoundedException ex = assertThrows(ReservationIdIsNotFoundedException.class,
                () -> validator.validatorCancel(ID_RESERVA, EMAIL, SEM_PRIVILEGIO));

        assertEquals("Não foi encontrado a reserva", ex.getMessage());
        verifyNoInteractions(userRepository);
    }

    @Test
    void validatorCancel_donoDaReserva_naoLanca() {
        when(repository.findById(ID_RESERVA))
                .thenReturn(Optional.of(reserva(ReserveStatus.ENVIADA_PARA_ANALISE, ID_DONO)));
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_DONO);

        assertDoesNotThrow(() -> validator.validatorCancel(ID_RESERVA, EMAIL, SEM_PRIVILEGIO));
    }

    @Test
    void validatorCancel_outroUsuarioSemPrivilegio_lancaSecurityException() {
        when(repository.findById(ID_RESERVA))
                .thenReturn(Optional.of(reserva(ReserveStatus.ENVIADA_PARA_ANALISE, ID_DONO)));
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_OUTRO);

        assertThrows(SecurityException.class,
                () -> validator.validatorCancel(ID_RESERVA, EMAIL, SEM_PRIVILEGIO));
    }

    @Test
    void validatorCancel_outroUsuarioDivisaoAdministrativa_naoLanca() {
        when(repository.findById(ID_RESERVA))
                .thenReturn(Optional.of(reserva(ReserveStatus.ENVIADA_PARA_ANALISE, ID_DONO)));
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_OUTRO);

        assertDoesNotThrow(() -> validator.validatorCancel(ID_RESERVA, EMAIL, DIVISAO_ADMINISTRATIVA));
    }

    @Test
    void validatorCancel_reservaJaCancelada_lancaReservationWasCancell() {
        when(repository.findById(ID_RESERVA))
                .thenReturn(Optional.of(reserva(ReserveStatus.CANCELADA, ID_DONO)));
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_DONO);

        assertThrows(ReservationWasCancell.class,
                () -> validator.validatorCancel(ID_RESERVA, EMAIL, SEM_PRIVILEGIO));
    }

    @Test
    void validatorCancel_reservaSemUsuarioVinculado_estouraNullPointer() {
        when(repository.findById(ID_RESERVA))
                .thenReturn(Optional.of(reserva(ReserveStatus.ENVIADA_PARA_ANALISE, null)));
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(ID_DONO);

        assertThrows(NullPointerException.class,
                () -> validator.validatorCancel(ID_RESERVA, EMAIL, SEM_PRIVILEGIO));
    }
}
