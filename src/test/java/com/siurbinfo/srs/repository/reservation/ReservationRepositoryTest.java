package com.siurbinfo.srs.repository.reservation;

import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.entity.room.RoomEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.enums.VehicleType;
import com.siurbinfo.srs.repository.room.RoomRepository;
import com.siurbinfo.srs.repository.user.ReservationsProjection;
import com.siurbinfo.srs.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReservationRepositoryTest {

    private static final LocalDate DIA = LocalDate.of(2031, 3, 10);

    @Autowired
    ReservationRepository repository;

    @Autowired
    ReservationAuditoriumRepository auditoriumRepository;

    @Autowired
    ReservationMeetingRoomRepository meetingRoomRepository;

    @Autowired
    ReservationFleetVehicleRepository fleetVehicleRepository;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    UserRepository userRepository;

    private UserEntity dono;

    @BeforeEach
    void setUp() {
        dono = persistUser("dono@teste.com");
    }

    private UserEntity persistUser(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        return userRepository.saveAndFlush(user);
    }

    private AuditoriumEntity persistAuditorium(UserEntity usuario, LocalDate data, LocalTime inicio, LocalTime fim) {
        AuditoriumEntity e = new AuditoriumEntity();
        e.setReservationDate(data);
        e.setTimeOpenAuditorium(LocalTime.of(8, 30));
        e.setReason("Palestra de teste");
        e.setStartTime(inicio);
        e.setEndTime(fim);
        e.setUserNameRequester("Fulano de Tal");
        e.setRequesterDepartment("TI");
        e.setNumPeople(50);
        e.setNecessaryScreen(true);
        e.setNecessaryCoffee(false);
        e.setUser(usuario);
        e.setReservationType(ReserveType.AUDITORIUM);
        e.setStatus(ReserveStatus.APROVADA);
        return auditoriumRepository.saveAndFlush(e);
    }

    private MeetingRoomEntity persistMeetingRoom(UserEntity usuario, LocalDate data, LocalTime inicio, LocalTime fim) {
        RoomEntity room = new RoomEntity();
        room.setFloor("ANDAR TESTE");
        room = roomRepository.saveAndFlush(room);

        MeetingRoomEntity e = new MeetingRoomEntity();
        e.setReservationDate(data);
        e.setRoom(room);
        e.setStartTime(inicio);
        e.setEndTime(fim);
        e.setUserNameRequester("Fulano de Tal");
        e.setRequesterDepartment("TI");
        e.setNumPeople(5);
        e.setIsCoffee(Boolean.FALSE);
        e.setUser(usuario);
        e.setReservationType(ReserveType.MEETING_ROOM);
        e.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        return meetingRoomRepository.saveAndFlush(e);
    }

    private FleetVehicleEntity persistFleet(UserEntity usuario, LocalDate data, LocalTime saida, LocalTime retorno) {
        FleetVehicleEntity e = new FleetVehicleEntity();
        e.setReservationDate(data);
        e.setExitTime(saida);
        e.setReason("Visita técnica");
        e.setDepartureAddress("Sede");
        e.setDestinationAddress("Cliente X");
        e.setIsOutsideTheMunicipality(false);
        e.setUserNameRequester("Fulano de Tal");
        e.setRequesterDepartment("TI");
        e.setContact("11999998888");
        e.setNecessaryReturn(retorno != null);
        e.setReturnTime(retorno);
        e.setNecessaryVan(false);
        e.setTypeVehicle(VehicleType.CARRO);
        e.setQuantityPassengers(3);
        e.setUser(usuario);
        e.setReservationType(ReserveType.FLEET_VEHICLE);
        e.setStatus(ReserveStatus.CANCELADA);
        return fleetVehicleRepository.saveAndFlush(e);
    }

    @Test
    void getAllReservations_juntaOsTresTiposNoIntervalo() {
        Long auditorio = persistAuditorium(dono, DIA, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();
        Long sala = persistMeetingRoom(dono, DIA, LocalTime.of(14, 0), LocalTime.of(15, 0)).getId();
        Long frota = persistFleet(dono, DIA, LocalTime.of(8, 0), LocalTime.of(18, 0)).getId();

        List<ReservationsProjection> rows = repository.getAllReservations(dono.getId(), DIA, DIA.plusDays(1));

        assertEquals(3, rows.size());
        assertEquals(List.of("AUDITORIUM", "FLEET_VEHICLE", "MEETING_ROOM"),
                rows.stream().map(ReservationsProjection::reservationType).toList());
        assertEquals(List.of(auditorio, frota, sala),
                rows.stream().map(ReservationsProjection::id).toList());
        rows.forEach(r -> {
            assertEquals(3L, r.totalReservations());
            assertEquals(dono.getId(), r.idUser());
            assertEquals(DIA, r.reservationDate());
        });
    }

    @Test
    void getAllReservations_mapeiaStatusEHorarios() {
        persistMeetingRoom(dono, DIA, LocalTime.of(14, 0), LocalTime.of(15, 0));

        ReservationsProjection sala = repository.getAllReservations(dono.getId(), DIA, DIA.plusDays(1)).getFirst();

        assertEquals("ENVIADA_PARA_ANALISE", sala.status());
        assertEquals(LocalTime.of(14, 0), sala.startTime());
        assertEquals(LocalTime.of(15, 0), sala.endTime());
        assertEquals("Fulano de Tal", sala.userNameRequester());
    }

    @Test
    void getAllReservations_frotaUsaSaidaERetornoComoHorarios() {
        persistFleet(dono, DIA, LocalTime.of(8, 0), LocalTime.of(18, 0));

        ReservationsProjection frota = repository.getAllReservations(dono.getId(), DIA, DIA.plusDays(1)).getFirst();

        assertEquals("CANCELADA", frota.status());
        assertEquals(LocalTime.of(8, 0), frota.startTime());
        assertEquals(LocalTime.of(18, 0), frota.endTime());
    }

    @Test
    void getAllReservations_frotaSemRetorno_endTimeNulo() {
        persistFleet(dono, DIA, LocalTime.of(8, 0), null);

        ReservationsProjection frota = repository.getAllReservations(dono.getId(), DIA, DIA.plusDays(1)).getFirst();

        assertNull(frota.endTime());
    }

    @Test
    void getAllReservations_naoTrazFrotaDeOutroUsuario() {
        UserEntity outro = persistUser("outro@teste.com");
        Long minha = persistFleet(dono, DIA, LocalTime.of(8, 0), null).getId();
        persistFleet(outro, DIA, LocalTime.of(10, 0), null);

        List<ReservationsProjection> rows = repository.getAllReservations(dono.getId(), DIA, DIA.plusDays(1));

        assertEquals(List.of(minha), rows.stream().map(ReservationsProjection::id).toList());
        assertEquals(1L, rows.getFirst().totalReservations());
    }

    @Test
    void getAllReservations_auditorioESalaDeOutroUsuarioAparecem_frotaNao() {
        UserEntity outro = persistUser("outro@teste.com");
        Long auditorioDoOutro = persistAuditorium(outro, DIA, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();
        Long salaDoOutro = persistMeetingRoom(outro, DIA, LocalTime.of(14, 0), LocalTime.of(15, 0)).getId();
        persistFleet(outro, DIA, LocalTime.of(8, 0), null);

        List<ReservationsProjection> rows = repository.getAllReservations(dono.getId(), DIA, DIA.plusDays(1));

        assertEquals(List.of(auditorioDoOutro, salaDoOutro),
                rows.stream().map(ReservationsProjection::id).toList());
        assertTrue(rows.stream().allMatch(r -> r.idUser().equals(outro.getId())));
        assertEquals(2L, rows.getFirst().totalReservations());
    }

    @Test
    void getAllReservations_inicioEFimInclusivos() {
        persistAuditorium(dono, DIA.minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0));
        persistAuditorium(dono, DIA, LocalTime.of(9, 0), LocalTime.of(10, 0));
        persistAuditorium(dono, DIA.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0));
        persistAuditorium(dono, DIA.plusDays(2), LocalTime.of(9, 0), LocalTime.of(10, 0));

        List<ReservationsProjection> rows = repository.getAllReservations(dono.getId(), DIA, DIA.plusDays(1));

        assertEquals(List.of(DIA, DIA.plusDays(1)),
                rows.stream().map(ReservationsProjection::reservationDate).toList());
        assertEquals(2L, rows.getFirst().totalReservations());
    }

    @Test
    void getAllReservations_semFim_trazTudoAPartirDoInicio() {
        persistAuditorium(dono, DIA.minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0));
        persistAuditorium(dono, DIA, LocalTime.of(9, 0), LocalTime.of(10, 0));
        persistFleet(dono, DIA.plusDays(30), LocalTime.of(8, 0), null);

        List<ReservationsProjection> rows = repository.getAllReservations(dono.getId(), DIA, null);

        assertEquals(List.of(DIA, DIA.plusDays(30)),
                rows.stream().map(ReservationsProjection::reservationDate).toList());
    }

    @Test
    void getAllReservations_ordenaPorDataAntesDoTipo() {
        persistMeetingRoom(dono, DIA, LocalTime.of(14, 0), LocalTime.of(15, 0));
        persistAuditorium(dono, DIA.plusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0));

        List<ReservationsProjection> rows = repository.getAllReservations(dono.getId(), DIA, DIA.plusDays(2));

        assertEquals(List.of("MEETING_ROOM", "AUDITORIUM"),
                rows.stream().map(ReservationsProjection::reservationType).toList());
    }

    @Test
    void getAllReservations_mesmoTipoEDia_ordenaPeloHorario() {
        Long tarde = persistAuditorium(dono, DIA, LocalTime.of(14, 0), LocalTime.of(15, 0)).getId();
        Long manha = persistAuditorium(dono, DIA, LocalTime.of(9, 0), LocalTime.of(10, 0)).getId();

        List<ReservationsProjection> rows = repository.getAllReservations(dono.getId(), DIA, DIA.plusDays(1));

        assertEquals(List.of(manha, tarde), rows.stream().map(ReservationsProjection::id).toList());
    }

    @Test
    void getAllReservations_semReservasNoIntervalo_devolveListaVazia() {
        persistAuditorium(dono, DIA, LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertTrue(repository.getAllReservations(dono.getId(), DIA.plusDays(1), DIA.plusDays(5)).isEmpty());
    }
}
