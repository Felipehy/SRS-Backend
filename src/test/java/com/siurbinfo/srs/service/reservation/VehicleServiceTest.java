package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleRequestDTO;
import com.siurbinfo.srs.dto.reservation.fleetVehicle.FleetVehicleResponseDTO;
import com.siurbinfo.srs.entity.Reserve.FleetVehicleEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.enums.VehicleType;
import com.siurbinfo.srs.exception.ReservationConflictException;
import com.siurbinfo.srs.exception.ReservationIdIsNotFoundedException;
import com.siurbinfo.srs.exception.ReservationWasApprove;
import com.siurbinfo.srs.exception.ReservationWasCancell;
import com.siurbinfo.srs.exception.SameDepartureAndDestinationException;
import com.siurbinfo.srs.mapper.reserve.VehicleMapperImpl;
import com.siurbinfo.srs.repository.reservation.ReservationFleetVehicleRepository;
import com.siurbinfo.srs.service.reservation.validators.ReservationValidator;
import com.siurbinfo.srs.service.reservation.validators.VehicleValidator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do VehicleService.
 *
 * O setup de estado (aprovar / reprovar / buscar / deletar) é feito inserindo a
 * entidade DIRETAMENTE no repositório, e não via createReservation(), para não
 * acoplar esses testes ao fluxo de criação — que hoje está quebrado por um bug de
 * PRODUÇÃO (ver relatório):
 *
 *   - BUG B: ReservationFleetVehicleRepository.existsByReservationDateAndHour usa
 *            a coluna "tfv.end_time", que NÃO existe em tbl_fleet_vehicles
 *            (o correto é "exit_time") -> InvalidDataAccessResourceUsageException.
 *            Isso quebra todo createReservation() e updateReservation(), que passam
 *            por essa query.
 *
 * Os testes marcados com "[BLOQUEADO POR BUG B]" só passam depois da correção.
 * NÃO alterei o código de produção.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({VehicleService.class, VehicleMapperImpl.class, VehicleValidator.class, ReservationValidator.class})
class VehicleServiceTest {

    @Autowired
    VehicleService service;

    @Autowired
    ReservationFleetVehicleRepository repository;

    private static final LocalDate DATE = LocalDate.of(2026, 8, 1);

    private FleetVehicleRequestDTO validDto() {
        return new FleetVehicleRequestDTO(
                DATE,                       // reservationDate
                LocalTime.of(8, 0),         // exitTime
                "Visita técnica",           // reason
                "Sede",                     // departureAddress
                "Cliente X",                // destinationAddress
                Boolean.TRUE,               // necessaryReturn
                LocalTime.of(18, 0),        // returnTime
                "Cliente X",                // departureAddressReturn
                "Sede",                     // destinationAddressReturn
                "11999998888",              // contact
                "Fulano de Tal",            // userNameRequester
                Boolean.FALSE,              // necessaryVan
                null,                       // reasonUseVan
                VehicleType.CARRO,          // typeVehicle
                3                           // quantityPassengers
        );
    }

    /** Insere uma reserva diretamente no banco, sem passar pelo service/validator. */
    private FleetVehicleEntity persistReservation(LocalDate date, LocalTime exitTime, String from, String to) {
        FleetVehicleEntity e = new FleetVehicleEntity();
        e.setReservationDate(date);
        e.setExitTime(exitTime);
        e.setReason("Visita técnica");
        e.setDepartureAddress(from);
        e.setDestinationAddress(to);
        e.setUserNameRequester("Fulano de Tal");
        e.setContact("11999998888");
        e.setNecessaryReturn(true);
        e.setReturnTime(LocalTime.of(18, 0));
        e.setNecessaryVan(false);
        e.setTypeVehicle(VehicleType.CARRO);
        e.setQuantityPassengers(3);
        e.setReservationType(ReserveType.FLEET_VEHICLE);
        e.setStatus(ReserveStatus.ENVIADA_PARA_ANALISE);
        return repository.saveAndFlush(e);
    }

    /** Insere uma reserva diretamente no banco com o status informado. */
    private FleetVehicleEntity persistWithStatus(ReserveStatus status) {
        FleetVehicleEntity e = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X");
        e.setStatus(status);
        return repository.saveAndFlush(e);
    }

    // ---------- createReservation ----------

    @Test
        // [BLOQUEADO POR BUG B] existsByReservationDateAndHour referencia coluna inexistente
    void createReservation_persistsWithDefaultStatusAndType() {
        service.createReservation(validDto());

        List<FleetVehicleEntity> all = repository.findAll();
        assertEquals(1, all.size());

        FleetVehicleEntity saved = all.getFirst();
        assertNotNull(saved.getId());
        assertEquals(ReserveType.FLEET_VEHICLE, saved.getReservationType());
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, saved.getStatus());
        assertEquals(VehicleType.CARRO, saved.getTypeVehicle());
        assertEquals(3, saved.getQuantityPassengers());
    }

    @Test
        // [BLOQUEADO POR BUG B] a query de conflito roda antes da checagem de endereços
    void createReservation_sameDepartureAndDestination_throws() {
        FleetVehicleRequestDTO dto = new FleetVehicleRequestDTO(
                DATE, LocalTime.of(8, 0), "Visita técnica", "Sede", "Sede",
                Boolean.TRUE, LocalTime.of(18, 0), "Sede", "Sede", "11999998888",
                "Fulano de Tal", Boolean.FALSE, null, VehicleType.CARRO, 3);

        assertThrows(SameDepartureAndDestinationException.class,
                () -> service.createReservation(dto));
        assertEquals(0, repository.count());
    }

    @Test
        // [BLOQUEADO POR BUG B] passa pela query de conflito quebrada
    void createReservation_conflictSameDate_throwsConflict() {
        // Reserva já existente com saída às 08:00
        persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X");

        // Nova reserva com saída às 09:00 (posterior) no mesmo dia -> conflito
        FleetVehicleRequestDTO dto = new FleetVehicleRequestDTO(
                DATE, LocalTime.of(9, 0), "Visita técnica", "Sede", "Cliente Y",
                Boolean.TRUE, LocalTime.of(18, 0), "Cliente Y", "Sede", "11999998888",
                "Fulano de Tal", Boolean.FALSE, null, VehicleType.CARRO, 3);

        assertThrows(ReservationConflictException.class,
                () -> service.createReservation(dto));
        assertEquals(1, repository.count());
    }

    // ---------- getReservationFromId / getAllReservation ----------

    @Test
    void getReservationFromId_returnsDto() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();

        FleetVehicleResponseDTO dto = service.getReservationFromId(id);

        assertEquals(id, dto.id());
        assertEquals(ReserveStatus.ENVIADA_PARA_ANALISE, dto.status());
    }

    @Test
    void getReservationFromId_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.getReservationFromId(999_999L));
    }

    @Test
    void getAllReservation_returnsAll() {
        persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X");

        List<FleetVehicleResponseDTO> all = service.getAllReservation();

        assertEquals(1, all.size());
    }

    // ---------- approve / reject ----------

    @Test
        // [BLOQUEADO POR BUG D] validatorApprove -> findStatusById devolve a entidade
    void approve_setsStatusApproved() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();

        service.approve(id);

        FleetVehicleEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.APROVADA, reloaded.getStatus());
    }

    @Test
        // [BLOQUEADO POR BUG D] guarda nova isPossibleApprove
    void approve_alreadyApproved_throws() {
        Long id = persistWithStatus(ReserveStatus.APROVADA).getId();

        assertThrows(ReservationWasApprove.class, () -> service.approve(id));
    }

    @Test
        // [BLOQUEADO POR BUG D] validatorReject -> findStatusById devolve a entidade
    void reject_setsStatusRejectedWithReason() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();

        service.reject(id, Map.of("reasonFailure", "Frota em manutenção"));

        FleetVehicleEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(ReserveStatus.REPROVADA, reloaded.getStatus());
        assertEquals("Frota em manutenção", reloaded.getReasonFailure());
    }

    @Test
        // [BLOQUEADO POR BUG D] guarda nova isPossibleReject
    void reject_alreadyCancelled_throws() {
        Long id = persistWithStatus(ReserveStatus.CANCELADA).getId();

        assertThrows(ReservationWasCancell.class,
                () -> service.reject(id, Map.of("reasonFailure", "Frota em manutenção")));
    }

    // ---------- updateReservation ----------

    @Test
        // [BLOQUEADO POR BUG B] validateUpdate passa pela query de conflito quebrada
    void updateReservation_updatesExisting() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();

        // A validação de update (isSameReservationDate) exige uma data DIFERENTE da atual.
        FleetVehicleRequestDTO update = new FleetVehicleRequestDTO(
                DATE.plusDays(1), LocalTime.of(7, 30), "Transporte de equipe", "Sede", "Evento",
                Boolean.FALSE, null, null, null, "11888887777", "Fulano",
                Boolean.TRUE, "Grupo grande", VehicleType.VAN, 12);

        service.updateReservation(id, update);

        FleetVehicleEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(12, reloaded.getQuantityPassengers());
        assertEquals(VehicleType.VAN, reloaded.getTypeVehicle());
        assertTrue(reloaded.isNecessaryVan());
        assertEquals("Transporte de equipe", reloaded.getReason());
    }

    @Test
    void updateReservation_sameDate_throwsConflict() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();

        // Mantendo a MESMA data, a nova validação isSameReservationDate dispara conflito
        // (essa checagem ocorre ANTES da query quebrada, então o teste passa hoje).
        assertThrows(ReservationConflictException.class,
                () -> service.updateReservation(id, validDto()));
    }

    @Test
    void updateReservation_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.updateReservation(999_999L, validDto()));
    }

    // ---------- deleteReserve ----------

    @Test
    void deleteReserve_removesExisting() {
        Long id = persistReservation(DATE, LocalTime.of(8, 0), "Sede", "Cliente X").getId();

        service.deleteReserve(id);

        assertFalse(repository.existsById(id));
    }

    @Test
    void deleteReserve_notFound_throws() {
        assertThrows(ReservationIdIsNotFoundedException.class,
                () -> service.deleteReserve(999_999L));
    }
}
