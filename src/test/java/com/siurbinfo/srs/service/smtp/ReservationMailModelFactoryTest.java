package com.siurbinfo.srs.service.smtp;

import com.siurbinfo.srs.dto.email.ReservationEmailDTO;
import com.siurbinfo.srs.entity.driver.DriverEntity;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.entity.room.RoomEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.enums.VehicleType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReservationMailModelFactoryTest {

    private final ReservationMailModelFactory factory = new ReservationMailModelFactory();

    private static final String USERNAME = "Fulano de Tal";
    private static final String URL = "https://srs.example.com/";
    private static final String SUPORTE = "reserva@example.com";

    private MeetingRoomEntity meetingRoom() {
        RoomEntity room = new RoomEntity();
        room.setFloor("12º andar");

        MeetingRoomEntity e = new MeetingRoomEntity();
        e.setId(42L);
        e.setRoom(room);
        e.setReservationType(ReserveType.MEETING_ROOM);
        e.setStatus(ReserveStatus.APROVADA);
        e.setReservationDate(LocalDate.of(2026, 9, 10));
        e.setStartTime(LocalTime.of(14, 0));
        e.setEndTime(LocalTime.of(15, 30));
        e.setUserNameRequester(USERNAME);
        e.setRequesterDepartment("SIURB");
        e.setNumPeople(8);
        e.setIsCoffee(Boolean.FALSE);
        return e;
    }

    private AuditoriumEntity auditorium() {
        AuditoriumEntity e = new AuditoriumEntity();
        e.setId(42L);
        e.setReservationType(ReserveType.AUDITORIUM);
        e.setStatus(ReserveStatus.APROVADA);
        e.setReservationDate(LocalDate.of(2026, 9, 10));
        e.setTimeOpenAuditorium(LocalTime.of(8, 0));
        e.setStartTime(LocalTime.of(9, 0));
        e.setEndTime(LocalTime.of(11, 0));
        e.setReason("Palestra de seguranca");
        e.setUserNameRequester(USERNAME);
        e.setRequesterDepartment("SIURB");
        e.setNumPeople(120);
        e.setNecessaryScreen(true);
        e.setNecessarySoundSystem(false);
        e.setNecessaryCoffee(false);
        return e;
    }

    private FleetVehicleEntity fleetVehicle() {
        DriverEntity driver = new DriverEntity();
        driver.setName("Motorista de Tal");
        driver.setModelCar("Fiat Doblo");
        driver.setLicensePlate("ABC1D23");

        FleetVehicleEntity e = new FleetVehicleEntity();
        e.setId(42L);
        e.setDriver(driver);
        e.setReservationType(ReserveType.FLEET_VEHICLE);
        e.setStatus(ReserveStatus.APROVADA);
        e.setReservationDate(LocalDate.of(2026, 9, 10));
        e.setExitTime(LocalTime.of(8, 0));
        e.setNecessaryReturn(true);
        e.setReturnTime(LocalTime.of(17, 0));
        e.setReason("Vistoria de obra");
        e.setUserNameRequester(USERNAME);
        e.setRequesterDepartment("SIURB");
        e.setDepartureAddress("Rua XV de Novembro, 165");
        e.setDestinationAddress("Av. Paulista, 1000");
        e.setContact("11 99999-0000");
        e.setTypeVehicle(VehicleType.CARRO);
        e.setQuantityPassengers(3);
        return e;
    }

    @Test
    void protocolo_eOIdComSeisDigitos() {
        assertEquals("000042", factory.toDto(auditorium()).protocol());
    }

    @Test
    void protocolo_idAcimaDeSeisDigitos_naoEtruncado() {
        AuditoriumEntity e = auditorium();
        e.setId(1_234_567L);

        assertEquals("1234567", factory.toDto(e).protocol());
    }

    @Test
    void protocolo_semId_saiNulo() {
        AuditoriumEntity e = auditorium();
        e.setId(null);

        assertNull(factory.toDto(e).protocol());
    }

    @Test
    void dataEHora_saemDaDataDaReservaMaisOHorario() {
        ReservationEmailDTO dto = factory.toDto(auditorium());

        assertEquals(LocalDateTime.of(2026, 9, 10, 9, 0), dto.startDateTime());
        assertEquals(LocalDateTime.of(2026, 9, 10, 11, 0), dto.endDateTime());
    }

    @Test
    void dataEHora_semHorario_saemNulas() {
        AuditoriumEntity e = auditorium();
        e.setStartTime(null);
        e.setEndTime(null);

        ReservationEmailDTO dto = factory.toDto(e);

        assertNull(dto.startDateTime());
        assertNull(dto.endDateTime());
    }

    @Test
    void instanteDoEvento_eConvertidoParaOFusoDeSaoPaulo() {
        Instant meioDiaUtc = LocalDateTime.of(2026, 9, 10, 12, 0).toInstant(java.time.ZoneOffset.UTC);

        assertEquals(LocalDateTime.of(2026, 9, 10, 9, 0), factory.toLocalDateTime(meioDiaUtc));
    }

    @Test
    void instanteDoEvento_nulo_continuaNulo() {
        assertNull(factory.toLocalDateTime(null));
    }

    @Test
    void salaDeReuniao_oAndarIdentificaASala() {
        assertEquals("12º andar", factory.toDto(meetingRoom()).floor());
    }

    @Test
    void salaDeReuniao_semSalaVinculada_naoQuebra() {
        MeetingRoomEntity e = meetingRoom();
        e.setRoom(null);

        assertNull(factory.toDto(e).floor());
    }

    @Test
    void salaDeReuniao_naoTemFinalidade() {
        assertNull(factory.toDto(meetingRoom()).purpose());
    }

    @Test
    void salaDeReuniao_quantidadeDePessoasVaiEmAttendees() {
        ReservationEmailDTO dto = factory.toDto(meetingRoom());

        assertEquals(8, dto.attendees());
        assertNull(dto.expectedAudience(), "expectedAudience e do auditorio");
    }

    @Test
    void auditorio_telaoESomSaoDuasLinhasIndependentes() {
        ReservationEmailDTO dto = factory.toDto(auditorium());

        assertEquals(Boolean.TRUE, dto.usesScreen());
        assertEquals(Boolean.FALSE, dto.usesSoundSystem());
    }

    @Test
    void auditorio_quantidadeDePessoasVaiEmExpectedAudience() {
        ReservationEmailDTO dto = factory.toDto(auditorium());

        assertEquals(120, dto.expectedAudience());
        assertNull(dto.attendees(), "attendees e da sala de reuniao");
    }

    @Test
    void auditorio_finalidadeSaiDoCampoReason() {
        assertEquals("Palestra de seguranca", factory.toDto(auditorium()).purpose());
    }

    @Test
    void veiculo_modeloEPlacaSaemDoMotorista() {
        ReservationEmailDTO dto = factory.toDto(fleetVehicle());

        assertEquals("Fiat Doblo", dto.vehicleModel());
        assertEquals("ABC1D23", dto.licensePlate());
        assertEquals("Motorista de Tal", dto.driverName());
    }

    @Test
    void veiculo_semMotorista_caiNaCategoriaDoVeiculo() {
        FleetVehicleEntity e = fleetVehicle();
        e.setDriver(null);

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals("CARRO", dto.vehicleModel());
        assertNull(dto.licensePlate());
        assertNull(dto.driverName());
    }

    @Test
    void veiculo_semRetornoPrevisto_naoTemHorarioDeFim() {
        FleetVehicleEntity e = fleetVehicle();
        e.setNecessaryReturn(false);

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals(LocalDateTime.of(2026, 9, 10, 8, 0), dto.startDateTime());
        assertNull(dto.endDateTime());
    }

    @Test
    void veiculo_naoOfereceCafe() {
        ReservationEmailDTO dto = factory.toDto(fleetVehicle());

        assertNull(dto.coffee());
        assertNull(dto.coffeeReason());
        assertNull(dto.coffeeRejectReason());
    }

    @Test
    void veiculo_saidaEDestinoSaoEnderecosDiferentes() {
        ReservationEmailDTO dto = factory.toDto(fleetVehicle());

        assertEquals("Rua XV de Novembro, 165", dto.departure());
        assertEquals("Av. Paulista, 1000", dto.destination());
    }

    @Test
    void veiculo_horaDeSaidaEHoraDeRetornoSaoDoisCamposDiferentes() {
        ReservationEmailDTO dto = factory.toDto(fleetVehicle());

        assertEquals(LocalTime.of(8, 0), dto.departureTime());
        assertEquals(LocalTime.of(17, 0), dto.returnTime());
    }

    @Test
    void veiculo_semRetorno_aindaLevaAHoraDeSaida() {
        FleetVehicleEntity e = fleetVehicle();
        e.setNecessaryReturn(false);

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals(LocalTime.of(8, 0), dto.departureTime());
        assertNull(dto.returnTime());
    }

    @Test
    void veiculo_ramalEContatoSaoNumerosDiferentes() {
        FleetVehicleEntity e = fleetVehicle();
        e.setTelephone("1611");

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals("1611", dto.requesterPhone());
        assertEquals("11 99999-0000", dto.contactPhone());
    }

    @Test
    void veiculo_comRetorno_levaHorarioEOsDoisEnderecos() {
        FleetVehicleEntity e = fleetVehicle();
        e.setDepartureAddressReturn("Av. Paulista, 1000");
        e.setDestinationAddressReturn("Rua XV de Novembro, 165");

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals(Boolean.TRUE, dto.hasReturn());
        assertEquals(LocalTime.of(17, 0), dto.returnTime());
        assertEquals("Av. Paulista, 1000", dto.returnDeparture());
        assertEquals("Rua XV de Novembro, 165", dto.returnDestination());
    }

    @Test
    void veiculo_semRetorno_naoLevaNenhumCampoDeRetorno() {
        FleetVehicleEntity e = fleetVehicle();
        e.setNecessaryReturn(false);
        e.setDepartureAddressReturn("Av. Paulista, 1000");
        e.setDestinationAddressReturn("Rua XV de Novembro, 165");

        ReservationEmailDTO dto = factory.toDto(e);

        assertNull(dto.hasReturn());
        assertNull(dto.returnTime());
        assertNull(dto.returnDeparture());
        assertNull(dto.returnDestination());
    }

    @Test
    void veiculo_comVan_levaAJustificativa() {
        FleetVehicleEntity e = fleetVehicle();
        e.setNecessaryVan(true);
        e.setReasonUseVan("Equipe de oito pessoas com equipamento");

        assertEquals("Equipe de oito pessoas com equipamento", factory.toDto(e).vanReason());
    }

    @Test
    void veiculo_semVan_naoLevaJustificativa() {
        FleetVehicleEntity e = fleetVehicle();
        e.setNecessaryVan(false);
        e.setReasonUseVan("Equipe de oito pessoas com equipamento");

        assertNull(factory.toDto(e).vanReason());
    }

    @Test
    void veiculo_foraDoMunicipio_levaARespostaEAJustificativa() {
        FleetVehicleEntity e = fleetVehicle();
        e.setIsOutsideTheMunicipality(true);
        e.setReasonOutsideTheMunicipality("Vistoria de obra em Guarulhos");

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals(Boolean.TRUE, dto.outsideMunicipality());
        assertEquals("Vistoria de obra em Guarulhos", dto.outsideMunicipalityReason());
    }

    @Test
    void veiculo_dentroDoMunicipio_aRespostaAindaSai() {
        FleetVehicleEntity e = fleetVehicle();
        e.setIsOutsideTheMunicipality(false);

        assertEquals(Boolean.FALSE, factory.toDto(e).outsideMunicipality());
    }

    @Test
    void veiculo_dentroDoMunicipio_naoLevaJustificativa() {
        FleetVehicleEntity e = fleetVehicle();
        e.setIsOutsideTheMunicipality(false);
        e.setReasonOutsideTheMunicipality("Vistoria de obra em Guarulhos");

        assertNull(factory.toDto(e).outsideMunicipalityReason());
    }

    @Test
    void veiculo_semResposta_naoAfirmaQueFicaNoMunicipio() {
        FleetVehicleEntity e = fleetVehicle();
        e.setIsOutsideTheMunicipality(null);

        assertNull(factory.toDto(e).outsideMunicipality());
    }

    @Test
    void auditorio_aberturaEUmTerceiroHorario_alemDoInicioEDoTermino() {
        ReservationEmailDTO dto = factory.toDto(auditorium());

        assertEquals(LocalTime.of(8, 0), dto.openingTime());
        assertEquals(LocalDateTime.of(2026, 9, 10, 9, 0), dto.startDateTime());
        assertEquals(LocalDateTime.of(2026, 9, 10, 11, 0), dto.endDateTime());
    }

    @Test
    void auditorio_motivoDaAbertura_saiEmCampoProprio() {
        AuditoriumEntity e = auditorium();
        e.setReasonOpenAuditorium("Montagem do palco e passagem de som");
        e.setObservation("Levar cadeiras extras");

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals("Montagem do palco e passagem de som", dto.openingReason());
        assertEquals("Levar cadeiras extras", dto.observation());
    }

    @Test
    void auditorio_semMotivoDaAbertura_naoGeraLinha() {
        assertNull(factory.toDto(auditorium()).openingReason());
    }

    @Test
    void auditorio_semPublicoExterno_soLevaOTotal() {
        ReservationEmailDTO dto = factory.toDto(auditorium());

        assertEquals(120, dto.expectedAudience());
        assertNull(dto.internalAudience());
        assertNull(dto.externalAudience());
    }

    @Test
    void auditorio_comPublicoExterno_quebraOTotalEmInternosEExternos() {
        AuditoriumEntity e = auditorium();
        e.setExternalPublic(true);
        e.setNumExternalPeople(30);

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals(120, dto.expectedAudience());
        assertEquals(90, dto.internalAudience());
        assertEquals(30, dto.externalAudience());
    }

    @Test
    void auditorio_publicoExternoSemONumero_naoInventaAQuebra() {
        AuditoriumEntity e = auditorium();
        e.setExternalPublic(true);
        e.setNumExternalPeople(null);

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals(120, dto.expectedAudience());
        assertNull(dto.internalAudience());
        assertNull(dto.externalAudience());
    }

    @Test
    void auditorio_externosMaiorQueOTotal_somemAsInternasEmVezDeSairNegativo() {
        AuditoriumEntity e = auditorium();
        e.setExternalPublic(true);
        e.setNumExternalPeople(200);

        ReservationEmailDTO dto = factory.toDto(e);

        assertNull(dto.internalAudience());
        assertEquals(200, dto.externalAudience());
    }

    @Test
    void ramalDoSolicitante_saiNasTresReservas() {
        MeetingRoomEntity sala = meetingRoom();
        sala.setTelephone("1611");
        AuditoriumEntity auditorio = auditorium();
        auditorio.setTelephone("1612");
        FleetVehicleEntity veiculo = fleetVehicle();
        veiculo.setTelephone("1613");

        assertEquals("1611", factory.toDto(sala).requesterPhone());
        assertEquals("1612", factory.toDto(auditorio).requesterPhone());
        assertEquals("1613", factory.toDto(veiculo).requesterPhone());
    }

    @Test
    void observacao_saiDaSalaEDoAuditorio() {
        MeetingRoomEntity sala = meetingRoom();
        sala.setObservation("Precisa de mesa em U");
        AuditoriumEntity auditorio = auditorium();
        auditorio.setObservation("Abertura às 8h para montagem do palco");

        assertEquals("Precisa de mesa em U", factory.toDto(sala).observation());
        assertEquals("Abertura às 8h para montagem do palco", factory.toDto(auditorio).observation());
    }

    @Test
    void observacao_naoExisteNaFrota() {
        assertNull(factory.toDto(fleetVehicle()).observation());
    }

    @Test
    void foraDoExpediente_justificativaSaiDaSalaEDoAuditorio() {
        MeetingRoomEntity sala = meetingRoom();
        sala.setReasonOutsideHours("Reunião com a diretoria emendou no fim do dia");
        AuditoriumEntity auditorio = auditorium();
        auditorio.setReasonOutsideHours("Audiência pública se estende até as 21h");

        assertEquals("Reunião com a diretoria emendou no fim do dia", factory.toDto(sala).outsideHoursReason());
        assertEquals("Audiência pública se estende até as 21h", factory.toDto(auditorio).outsideHoursReason());
    }

    @Test
    void foraDoExpediente_dentroDoExpediente_naoGeraLinha() {
        assertNull(factory.toDto(meetingRoom()).outsideHoursReason());
        assertNull(factory.toDto(auditorium()).outsideHoursReason());
    }

    @Test
    void foraDoExpediente_naoExisteNaFrota() {
        assertNull(factory.toDto(fleetVehicle()).outsideHoursReason());
    }

    @Test
    void foraDoExpediente_jaVaiNoEmailDeSolicitacaoRecebida() {
        MeetingRoomEntity sala = meetingRoom();
        sala.setReasonOutsideHours("Reunião com a diretoria emendou no fim do dia");

        assertEquals("Reunião com a diretoria emendou no fim do dia",
                factory.toRequestedDto(sala).outsideHoursReason());
    }

    @Test
    void foraDoExpediente_naoSeConfundeComOMotivoDaAbertura() {
        AuditoriumEntity e = auditorium();
        e.setReasonOpenAuditorium("Montagem do palco e passagem de som");
        e.setReasonOutsideHours("Audiência pública se estende até as 21h");

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals("Montagem do palco e passagem de som", dto.openingReason());
        assertEquals("Audiência pública se estende até as 21h", dto.outsideHoursReason());
    }

    @Test
    void cafe_noEmailDeSolicitacao_dizApenasSeFoiPedido() {
        MeetingRoomEntity semCafe = meetingRoom();
        MeetingRoomEntity comCafe = meetingRoom();
        comCafe.setIsCoffee(Boolean.TRUE);

        assertEquals("Não", factory.toRequestedDto(semCafe).coffee());
        assertEquals("Sim", factory.toRequestedDto(comCafe).coffee());
    }

    @Test
    void cafe_motivoVaiEmCampoProprio_foraDoSimOuNao() {
        MeetingRoomEntity e = meetingRoom();
        e.setIsCoffee(Boolean.TRUE);
        e.setReasonCoffee("Reunião com a diretoria");

        ReservationEmailDTO dto = factory.toRequestedDto(e);

        assertEquals("Sim", dto.coffee());
        assertEquals("Reunião com a diretoria", dto.coffeeReason());
    }

    @Test
    void cafe_naoPedido_naoLevaMotivo() {
        MeetingRoomEntity e = meetingRoom();
        e.setIsCoffee(Boolean.FALSE);
        e.setReasonCoffee("Reunião com a diretoria");

        assertNull(factory.toRequestedDto(e).coffeeReason());
        assertNull(factory.toDto(e).coffeeReason());
    }

    @Test
    void cafe_motivoEmBranco_naoGeraLinhaVazia() {
        MeetingRoomEntity e = meetingRoom();
        e.setIsCoffee(Boolean.TRUE);
        e.setReasonCoffee("   ");

        assertNull(factory.toRequestedDto(e).coffeeReason());
    }

    @Test
    void cafe_noEmailDeSolicitacao_naoCarregaOEstadoDaAprovacao() {
        MeetingRoomEntity e = meetingRoom();
        e.setIsCoffee(Boolean.TRUE);
        e.setIsApproveCoffee(Boolean.TRUE);

        assertEquals("Sim", factory.toRequestedDto(e).coffee());
    }

    @Test
    void cafe_isCoffeeNulo_contaComoNaoPedido() {
        MeetingRoomEntity e = meetingRoom();
        e.setIsCoffee(null);

        assertEquals("Não", factory.toRequestedDto(e).coffee());
        assertEquals("Não", factory.toDto(e).coffee());
    }

    @Test
    void cafe_naoPedido_saiComoNaoNosDemaisEmails() {
        assertEquals("Não", factory.toDto(meetingRoom()).coffee());
        assertEquals("Não", factory.toDto(auditorium()).coffee());
    }

    @Test
    void cafe_pedidoEAindaNaoAnalisado_apareceComoAguardando() {
        MeetingRoomEntity e = meetingRoom();
        e.setIsCoffee(Boolean.TRUE);

        assertEquals("Sim (aguardando aprovação)", factory.toDto(e).coffee());
    }

    @Test
    void cafe_aprovado_apareceComoAprovado() {
        MeetingRoomEntity e = meetingRoom();
        e.setIsCoffee(Boolean.TRUE);
        e.setIsApproveCoffee(Boolean.TRUE);
        e.setReasonCoffee("Reunião com a diretoria");

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals("Sim (aprovado)", dto.coffee());
        assertEquals("Reunião com a diretoria", dto.coffeeReason());
    }

    @Test
    void cafe_reprovado_poeOMotivoDaRecusaEmLinhaPropria() {
        MeetingRoomEntity e = meetingRoom();
        e.setIsCoffee(Boolean.TRUE);
        e.setIsApproveCoffee(Boolean.FALSE);
        e.setReasonRejectCoffee("Sem verba no mês");

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals("Sim (reprovado)", dto.coffee());
        assertEquals("Sem verba no mês", dto.coffeeRejectReason());
    }

    @Test
    void cafe_reprovadoSemMotivo_naoGeraLinhaVazia() {
        MeetingRoomEntity e = meetingRoom();
        e.setIsCoffee(Boolean.TRUE);
        e.setIsApproveCoffee(Boolean.FALSE);
        e.setReasonRejectCoffee("   ");

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals("Sim (reprovado)", dto.coffee());
        assertNull(dto.coffeeRejectReason());
    }

    @Test
    void cafe_aprovado_naoLevaMotivoDeRecusa() {
        MeetingRoomEntity e = meetingRoom();
        e.setIsCoffee(Boolean.TRUE);
        e.setIsApproveCoffee(Boolean.TRUE);
        e.setReasonRejectCoffee("Sem verba no mês");

        assertNull(factory.toDto(e).coffeeRejectReason());
    }

    @Test
    void cafe_noEmailDeSolicitacao_naoLevaMotivoDeRecusa() {
        MeetingRoomEntity e = meetingRoom();
        e.setIsCoffee(Boolean.TRUE);
        e.setIsApproveCoffee(Boolean.FALSE);
        e.setReasonRejectCoffee("Sem verba no mês");

        assertNull(factory.toRequestedDto(e).coffeeRejectReason());
    }

    @Test
    void cafe_doAuditorio_segueAMesmaRegraDaSala() {
        AuditoriumEntity e = auditorium();
        e.setNecessaryCoffee(true);
        e.setIsApproveCoffeeBreak(Boolean.FALSE);
        e.setReasonRejectCoffee("Sem verba no mês");
        e.setReasonCoffee("Palestra de dia inteiro");

        ReservationEmailDTO dto = factory.toDto(e);

        assertEquals("Sim (reprovado)", dto.coffee());
        assertEquals("Palestra de dia inteiro", dto.coffeeReason());
        assertEquals("Sem verba no mês", dto.coffeeRejectReason());
    }

    @Test
    void model_temAsChavesQueTodoTemplateLe() {
        Map<String, Object> model = factory.requested(USERNAME, URL, SUPORTE, meetingRoom());

        assertEquals(USERNAME, model.get("username"));
        assertEquals(URL, model.get("urlAccompany"));
        assertEquals(SUPORTE, model.get("email"));
        assertTrue(model.get("reservation") instanceof ReservationEmailDTO,
                "os templates leem os dados da reserva pela chave reservation");
    }

    @Test
    void modelDeSolicitacao_levaAHoraDoPedido() {
        MeetingRoomEntity e = meetingRoom();
        e.setDateCreate(LocalDateTime.of(2026, 9, 10, 12, 0).toInstant(java.time.ZoneOffset.UTC));

        Map<String, Object> model = factory.requested(USERNAME, URL, SUPORTE, e);

        assertEquals(LocalDateTime.of(2026, 9, 10, 9, 0), model.get("requestedAt"));
    }

    @Test
    void modelDeAprovacao_levaQuemAprovouEQuando() {
        AuditoriumEntity e = auditorium();
        e.setDateAnalysis(LocalDateTime.of(2026, 9, 10, 12, 0).toInstant(java.time.ZoneOffset.UTC));

        Map<String, Object> model = factory.approved(USERNAME, URL, SUPORTE, "SIURB - Reservas", e);

        assertEquals("SIURB - Reservas", model.get("approvedBy"));
        assertEquals(LocalDateTime.of(2026, 9, 10, 9, 0), model.get("approvedAt"));
    }

    @Test
    void modelDeReprovacao_levaOMotivoDaEntidade() {
        AuditoriumEntity e = auditorium();
        e.setReasonFailure("Auditório em manutenção.");
        e.setDateAnalysis(Instant.now());

        Map<String, Object> model = factory.rejected(USERNAME, URL, SUPORTE, "SIURB - Reservas", e);

        assertEquals("Auditório em manutenção.", model.get("rejectReason"));
        assertEquals("SIURB - Reservas", model.get("rejectedBy"));
    }

    @Test
    void modelDeCancelamento_camposSemOrigemVaoComoNull() {
        Map<String, Object> model = factory.cancelled(USERNAME, URL, SUPORTE, meetingRoom());

        assertTrue(model.containsKey("cancelledBy"));
        assertNull(model.get("cancelledBy"));
        assertNull(model.get("cancelledAt"));
        assertNull(model.get("cancelReason"));
    }

    @Test
    void modelDeFinalizacao_camposSemOrigemVaoComoNull() {
        Map<String, Object> model = factory.finished(USERNAME, URL, SUPORTE, meetingRoom());

        assertTrue(model.containsKey("finishedAt"));
        assertNull(model.get("finishedAt"));
        assertNull(model.get("finishNotes"));
    }

    @Test
    void modelDeCancelamento_naoLevaODadoDaAnalise() {
        MeetingRoomEntity e = meetingRoom();
        e.setUserNameApprover("Beltrano de Tal");
        e.setDateAnalysis(Instant.now());

        Map<String, Object> model = factory.cancelled(USERNAME, URL, SUPORTE, e);

        assertFalse(model.containsValue("Beltrano de Tal"),
                "o aprovador nao pode aparecer como quem cancelou");
    }

    @Test
    void coffeeBreak_levaOsDadosDoPedido() {
        AuditoriumEntity e = auditorium();
        e.setFoodSetupTime(LocalTime.of(8, 30));
        e.setServiceTime(LocalTime.of(9, 0));

        Map<String, Object> model = factory.coffeeBreakApproved(e, SUPORTE);

        assertEquals(SUPORTE, model.get("supportEmail"));
        assertEquals(120, model.get("numPeople"));
        assertEquals("Palestra de seguranca", model.get("subjectEvent"));
        assertEquals(LocalDate.of(2026, 9, 10), model.get("date"));
        assertEquals(LocalTime.of(8, 30), model.get("foodSetupTime"));
        assertEquals(LocalTime.of(9, 0), model.get("serviceTime"));
        assertTrue(model.get("locale") != null && model.get("address") != null,
                "o fornecedor precisa saber onde entregar");
    }

    @Test
    void coffeeBreak_numeroDePessoasEOTotal_semSomarOsExternos() {
        AuditoriumEntity e = auditorium();
        e.setExternalPublic(true);
        e.setNumExternalPeople(30);

        Map<String, Object> model = factory.coffeeBreakApproved(e, SUPORTE);

        assertEquals(120, model.get("numPeople"));
    }
}
