package com.siurbinfo.srs.repository.user;

import com.siurbinfo.srs.entity.department.DepartmentEntity;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.entity.room.RoomEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.enums.VehicleType;
import com.siurbinfo.srs.repository.reservation.ReservationAuditoriumRepository;
import com.siurbinfo.srs.repository.reservation.ReservationFleetVehicleRepository;
import com.siurbinfo.srs.repository.reservation.ReservationMeetingRoomRepository;
import com.siurbinfo.srs.repository.room.RoomRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    private static final String EMAIL = "cores@teste.com";

    @Autowired
    UserRepository repository;

    @Autowired
    EntityManager entityManager;

    private UserEntity persistUser(String email) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        return repository.saveAndFlush(user);
    }

    @Test
    void setColors_thenGetColors_roundTrip() {
        persistUser(EMAIL);

        repository.setColors(EMAIL, "#111111", "#222222", "#333333");

        List<Object[]> rows = repository.getColors(EMAIL);
        assertEquals(1, rows.size());

        Object[] colors = rows.getFirst();
        assertEquals(3, colors.length);
        assertEquals("#111111", colors[0]);
        assertEquals("#222222", colors[1]);
        assertEquals("#333333", colors[2]);
    }

    @Test
    void setColors_updatesOnlyTargetUser() {
        persistUser(EMAIL);
        persistUser("outro@teste.com");

        repository.setColors(EMAIL, "#111111", "#222222", "#333333");

        Object[] other = repository.getColors("outro@teste.com").getFirst();
        assertNull(other[0]);
        assertNull(other[1]);
        assertNull(other[2]);
    }

    @Test
    void getColors_userNeverSetColors_returnsRowOfNulls() {
        persistUser(EMAIL);

        List<Object[]> rows = repository.getColors(EMAIL);
        assertEquals(1, rows.size());

        Object[] colors = rows.getFirst();
        assertEquals(3, colors.length);
        assertNull(colors[0]);
        assertNull(colors[1]);
        assertNull(colors[2]);
    }

    @Test
    void getColors_unknownEmail_returnsEmptyList() {
        assertTrue(repository.getColors("nao-existe@teste.com").isEmpty());
    }

    @Test
    void deleteByEmail_marcaDeletedAtSemApagarALinha() {
        Long id = persistUser(EMAIL).getId();
        Instant deletedAt = Instant.parse("2026-09-29T19:00:00Z");

        repository.deleteByEmail(EMAIL, deletedAt);

        assertTrue(repository.existsById(id));
        Number marcados = (Number) entityManager.createNativeQuery(
                        "select count(*) from tbl_user where id = :id and deleted_at = :deletedAt")
                .setParameter("id", id)
                .setParameter("deletedAt", deletedAt)
                .getSingleResult();
        assertEquals(1, marcados.intValue());
    }

    @Test
    void getIdByEmail_usuarioAtivo_devolveId() {
        Long id = persistUser(EMAIL).getId();

        assertEquals(id, repository.getIdByEmail(EMAIL));
    }

    @Test
    void getIdByEmail_usuarioExcluido_devolveNull() {
        persistUser(EMAIL);
        repository.deleteByEmail(EMAIL, Instant.parse("2026-09-29T19:00:00Z"));

        assertNull(repository.getIdByEmail(EMAIL));
    }

    @Test
    void getIdByEmail_emailRecriadoDepoisDeExcluido_devolveOAtivo() {
        Long antigo = persistUser(EMAIL).getId();
        repository.deleteByEmail(EMAIL, Instant.parse("2026-09-29T19:00:00Z"));
        Long novo = persistUser(EMAIL).getId();

        assertNotEquals(antigo, novo);
        assertEquals(novo, repository.getIdByEmail(EMAIL));
    }

    @Test
    void deleteByEmail_naoMexeEmOutroUsuario() {
        Long outroId = persistUser("outro@teste.com").getId();
        persistUser(EMAIL);

        repository.deleteByEmail(EMAIL, Instant.parse("2026-09-29T19:00:00Z"));

        Number marcados = (Number) entityManager.createNativeQuery(
                        "select count(*) from tbl_user where id = :id and deleted_at is not null")
                .setParameter("id", outroId)
                .getSingleResult();
        assertEquals(0, marcados.intValue());
    }

    private static final Instant EXCLUIDO_EM = Instant.parse("2026-09-29T19:00:00Z");

    private Long recriarUsuarioComLinhaExcluidaPreenchida() {
        DepartmentEntity departamento = new DepartmentEntity();
        departamento.setName("Departamento antigo");
        entityManager.persist(departamento);

        Long excluido = persistUser(EMAIL).getId();
        repository.setImgInUser("antiga.png", EMAIL);
        repository.setColors(EMAIL, "#111111", "#222222", "#333333");
        repository.setNavbar(true, EMAIL);
        repository.setFirstLogin(false, EMAIL);
        repository.setDepartment(departamento.getId(), EMAIL);
        repository.deleteByEmail(EMAIL, EXCLUIDO_EM);

        persistUser(EMAIL);
        entityManager.clear();
        return excluido;
    }

    private Object colunaDaLinha(String coluna, Long id) {
        return entityManager.createNativeQuery("select " + coluna + " from tbl_user where id = :id")
                .setParameter("id", id)
                .getSingleResult();
    }

    @Test
    void getUserByEmail_emailRecriado_devolveOAtivo() {
        Long excluido = recriarUsuarioComLinhaExcluidaPreenchida();

        UserEntity ativo = repository.getUserByEmail(EMAIL);

        assertNotEquals(excluido, ativo.getId());
        assertEquals(repository.getIdByEmail(EMAIL), ativo.getId());
    }

    @Test
    void getUserByEmail_soComLinhaExcluida_devolveNull() {
        persistUser(EMAIL);
        repository.deleteByEmail(EMAIL, EXCLUIDO_EM);

        assertNull(repository.getUserByEmail(EMAIL));
    }

    @Test
    void getIdDepartmentByEmail_emailRecriado_ignoraODepartamentoDaLinhaExcluida() {
        recriarUsuarioComLinhaExcluidaPreenchida();

        assertNull(repository.getIdDepartmentByEmail(EMAIL));
    }

    @Test
    void getNavbar_emailRecriado_devolveODoAtivo() {
        recriarUsuarioComLinhaExcluidaPreenchida();

        assertFalse(repository.getNavbar(EMAIL));
    }

    @Test
    void getImgInUser_emailRecriado_naoDevolveAFotoDaLinhaExcluida() {
        recriarUsuarioComLinhaExcluidaPreenchida();

        assertNull(repository.getImgInUser(EMAIL));
    }

    @Test
    void getColors_emailRecriado_devolveSoALinhaDoAtivo() {
        recriarUsuarioComLinhaExcluidaPreenchida();

        List<Object[]> rows = repository.getColors(EMAIL);

        assertEquals(1, rows.size());
        assertNull(rows.getFirst()[0]);
    }

    @Test
    void escritasPorEmail_emailRecriado_naoMexemNaLinhaExcluida() {
        Long excluido = recriarUsuarioComLinhaExcluidaPreenchida();
        DepartmentEntity novoDepartamento = new DepartmentEntity();
        novoDepartamento.setName("Departamento novo");
        entityManager.persist(novoDepartamento);

        repository.setImgInUser("nova.png", EMAIL);
        repository.setColors(EMAIL, "#aaaaaa", "#bbbbbb", "#cccccc");
        repository.setNavbar(false, EMAIL);
        repository.setFirstLogin(true, EMAIL);
        repository.setDepartment(novoDepartamento.getId(), EMAIL);
        repository.deleteImgByEmail(EMAIL);
        repository.deleteByEmail(EMAIL, Instant.parse("2026-10-01T12:00:00Z"));

        assertEquals("antiga.png", colunaDaLinha("img", excluido));
        assertEquals("#111111", colunaDaLinha("color_auditorium", excluido));
        assertEquals(Boolean.TRUE, colunaDaLinha("is_horizontal_navbar", excluido));
        assertEquals(Boolean.FALSE, colunaDaLinha("first_login", excluido));
        assertNotEquals(novoDepartamento.getId(), ((Number) colunaDaLinha("id_department", excluido)).longValue());
        Number mesmaData = (Number) entityManager.createNativeQuery(
                        "select count(*) from tbl_user where id = :id and deleted_at = :deletedAt")
                .setParameter("id", excluido)
                .setParameter("deletedAt", EXCLUIDO_EM)
                .getSingleResult();
        assertEquals(1, mesmaData.intValue());
    }

    @Test
    void setImgInUser_thenGetImgInUser_roundTrip() {
        persistUser(EMAIL);

        repository.setImgInUser("pasta/foto.png", EMAIL);

        assertEquals("pasta/foto.png", repository.getImgInUser(EMAIL));
    }

    @Test
    void getEmailById_devolveOEmailDoUsuario() {
        Long id = persistUser(EMAIL).getId();

        assertEquals(EMAIL, repository.getEmailById(id));
    }

    @Test
    void getEmailById_idDeOutroUsuario_naoVazaOEmailErrado() {
        persistUser(EMAIL);
        Long outro = persistUser("outro@teste.com").getId();

        assertEquals("outro@teste.com", repository.getEmailById(outro));
    }

    @Test
    void getEmailById_idInexistente_devolveNull() {
        assertNull(repository.getEmailById(999_999L));
    }

    private static final LocalDate DIA = LocalDate.of(2026, 8, 1);

    private static final List<String> TODOS_OS_TIPOS = List.of("AUDITORIUM", "MEETING_ROOM", "FLEET_VEHICLE");

    @Autowired
    ReservationAuditoriumRepository auditoriumRepository;

    @Autowired
    ReservationMeetingRoomRepository meetingRoomRepository;

    @Autowired
    ReservationFleetVehicleRepository fleetVehicleRepository;

    @Autowired
    RoomRepository roomRepository;

    private AuditoriumEntity persistAuditorium(UserEntity dono, LocalDate data, LocalTime inicio, LocalTime fim) {
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
        e.setUser(dono);
        e.setReservationType(ReserveType.AUDITORIUM);
        e.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        return auditoriumRepository.saveAndFlush(e);
    }

    private MeetingRoomEntity persistMeetingRoom(UserEntity dono, LocalDate data, LocalTime inicio, LocalTime fim) {
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
        e.setUser(dono);
        e.setReservationType(ReserveType.MEETING_ROOM);
        e.setStatus(ReserveStatus.APROVADA);
        return meetingRoomRepository.saveAndFlush(e);
    }

    private FleetVehicleEntity persistFleet(UserEntity dono, LocalDate data, LocalTime saida, LocalTime retorno) {
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
        e.setUser(dono);
        e.setReservationType(ReserveType.FLEET_VEHICLE);
        e.setStatus(ReserveStatus.CANCELADA);
        return fleetVehicleRepository.saveAndFlush(e);
    }

    @Test
    void getReservationsByIdUser_juntaOsTresTiposDoUsuario() {
        UserEntity dono = persistUser(EMAIL);
        Long auditorio = persistAuditorium(dono, DIA, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();
        Long sala = persistMeetingRoom(dono, DIA, LocalTime.of(14, 0), LocalTime.of(15, 0)).getId();
        Long frota = persistFleet(dono, DIA, LocalTime.of(8, 0), LocalTime.of(18, 0)).getId();

        List<ReservationsProjection> rows = repository.getReservationsByIdUser(dono.getId(), null, 0L, TODOS_OS_TIPOS);

        assertEquals(3, rows.size());
        assertEquals(List.of("AUDITORIUM", "FLEET_VEHICLE", "MEETING_ROOM"),
                rows.stream().map(ReservationsProjection::reservationType).toList());
        assertEquals(List.of(auditorio, frota, sala),
                rows.stream().map(ReservationsProjection::id).toList());
        rows.forEach(r -> {
            assertEquals(3L, r.totalReservations());
            assertEquals(dono.getId(), r.idUser());
            assertEquals(DIA, r.reservationDate());
            assertEquals("Fulano de Tal", r.userNameRequester());
        });
    }

    @Test
    void getReservationsByIdUser_mapeiaStatusEHorarios() {
        UserEntity dono = persistUser(EMAIL);
        persistMeetingRoom(dono, DIA, LocalTime.of(14, 0), LocalTime.of(15, 0));

        ReservationsProjection sala = repository.getReservationsByIdUser(dono.getId(), null, 0L, TODOS_OS_TIPOS).getFirst();

        assertEquals("APROVADA", sala.status());
        assertEquals(LocalTime.of(14, 0), sala.startTime());
        assertEquals(LocalTime.of(15, 0), sala.endTime());
    }

    @Test
    void getReservationsByIdUser_frotaUsaSaidaERetornoComoHorarios() {
        UserEntity dono = persistUser(EMAIL);
        persistFleet(dono, DIA, LocalTime.of(8, 0), LocalTime.of(18, 0));

        ReservationsProjection frota = repository.getReservationsByIdUser(dono.getId(), null, 0L, TODOS_OS_TIPOS).getFirst();

        assertEquals("CANCELADA", frota.status());
        assertEquals(LocalTime.of(8, 0), frota.startTime());
        assertEquals(LocalTime.of(18, 0), frota.endTime());
    }

    @Test
    void getReservationsByIdUser_frotaSemRetorno_endTimeNulo() {
        UserEntity dono = persistUser(EMAIL);
        persistFleet(dono, DIA, LocalTime.of(8, 0), null);

        ReservationsProjection frota = repository.getReservationsByIdUser(dono.getId(), null, 0L, TODOS_OS_TIPOS).getFirst();

        assertNull(frota.endTime());
    }

    @Test
    void getReservationsByIdUser_naoTrazReservaDeOutroUsuario() {
        UserEntity dono = persistUser(EMAIL);
        UserEntity outro = persistUser("outro@teste.com");
        persistAuditorium(dono, DIA, LocalTime.of(9, 0), LocalTime.of(11, 0));
        persistAuditorium(outro, DIA, LocalTime.of(13, 0), LocalTime.of(15, 0));
        persistFleet(outro, DIA, LocalTime.of(8, 0), null);

        List<ReservationsProjection> rows = repository.getReservationsByIdUser(dono.getId(), null, 0L, TODOS_OS_TIPOS);

        assertEquals(1, rows.size());
        assertEquals(1L, rows.getFirst().totalReservations());
    }

    @Test
    void getReservationsByIdUser_comData_filtraOsTresTipos() {
        UserEntity dono = persistUser(EMAIL);
        persistAuditorium(dono, DIA, LocalTime.of(9, 0), LocalTime.of(11, 0));
        persistMeetingRoom(dono, DIA.plusDays(1), LocalTime.of(14, 0), LocalTime.of(15, 0));
        persistFleet(dono, DIA.plusDays(1), LocalTime.of(8, 0), null);

        List<ReservationsProjection> rows = repository.getReservationsByIdUser(dono.getId(), DIA.plusDays(1), 0L, TODOS_OS_TIPOS);

        assertEquals(2, rows.size());
        assertTrue(rows.stream().allMatch(r -> r.reservationDate().equals(DIA.plusDays(1))));
        assertEquals(2L, rows.getFirst().totalReservations());
    }

    @Test
    void getReservationsByIdUser_semData_ordenaPorDataPrimeiro() {
        UserEntity dono = persistUser(EMAIL);
        persistMeetingRoom(dono, DIA.plusDays(2), LocalTime.of(14, 0), LocalTime.of(15, 0));
        persistAuditorium(dono, DIA, LocalTime.of(9, 0), LocalTime.of(11, 0));

        List<ReservationsProjection> rows = repository.getReservationsByIdUser(dono.getId(), null, 0L, TODOS_OS_TIPOS);

        assertEquals(List.of(DIA, DIA.plusDays(2)),
                rows.stream().map(ReservationsProjection::reservationDate).toList());
    }

    @Test
    void getReservationsByIdUser_paginaDe10EmDez_totalContaTudo() {
        UserEntity dono = persistUser(EMAIL);
        for (int i = 0; i < 12; i++) {
            persistFleet(dono, DIA.plusDays(i), LocalTime.of(8, 0), null);
        }

        List<ReservationsProjection> primeira = repository.getReservationsByIdUser(dono.getId(), null, 0L, TODOS_OS_TIPOS);
        List<ReservationsProjection> segunda = repository.getReservationsByIdUser(dono.getId(), null, 10L, TODOS_OS_TIPOS);

        assertEquals(10, primeira.size());
        assertEquals(2, segunda.size());
        assertEquals(12L, primeira.getFirst().totalReservations());
        assertEquals(12L, segunda.getFirst().totalReservations());
        assertEquals(DIA.plusDays(10), segunda.getFirst().reservationDate());
    }

    @Test
    void getReservationsByIdUser_usuarioSemReserva_devolveListaVazia() {
        UserEntity dono = persistUser(EMAIL);

        assertTrue(repository.getReservationsByIdUser(dono.getId(), null, 0L, TODOS_OS_TIPOS).isEmpty());
    }

    @Test
    void getReservationsByIdUser_paginaAlemDoFim_devolveListaVazia() {
        UserEntity dono = persistUser(EMAIL);
        persistAuditorium(dono, DIA, LocalTime.of(9, 0), LocalTime.of(11, 0));

        assertTrue(repository.getReservationsByIdUser(dono.getId(), null, 10L, TODOS_OS_TIPOS).isEmpty());
    }

    @Test
    void getReservationsByIdUser_filtraPorTipo_eTotalContaSoOsFiltrados() {
        UserEntity dono = persistUser(EMAIL);
        Long auditorio = persistAuditorium(dono, DIA, LocalTime.of(9, 0), LocalTime.of(11, 0)).getId();
        persistMeetingRoom(dono, DIA, LocalTime.of(14, 0), LocalTime.of(15, 0));
        Long frota = persistFleet(dono, DIA, LocalTime.of(8, 0), null).getId();

        List<ReservationsProjection> rows =
                repository.getReservationsByIdUser(dono.getId(), null, 0L, List.of("AUDITORIUM", "FLEET_VEHICLE"));

        assertEquals(List.of(auditorio, frota), rows.stream().map(ReservationsProjection::id).toList());
        assertEquals(2L, rows.getFirst().totalReservations());
    }

    @Test
    void usuarioNovo_nasceComFirstLoginTrue() {
        persistUser(EMAIL);
        entityManager.clear();

        assertTrue(repository.getUserByEmail(EMAIL).getIsFirstLogin());
    }

    @Test
    void setFirstLogin_gravaSoParaOUsuarioInformado() {
        persistUser(EMAIL);
        persistUser("outro@teste.com");

        repository.setFirstLogin(false, EMAIL);

        entityManager.clear();
        assertFalse(repository.getUserByEmail(EMAIL).getIsFirstLogin());
        assertTrue(repository.getUserByEmail("outro@teste.com").getIsFirstLogin());
    }
}
