package com.siurbinfo.srs.service.smtp;

import com.siurbinfo.srs.entity.config.ConfigEntity;
import com.siurbinfo.srs.entity.driver.DriverEntity;
import com.siurbinfo.srs.entity.email.EmailEntity;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.entity.room.RoomEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.EmailStatus;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.enums.VehicleType;
import com.siurbinfo.srs.exception.EntityNotFoundException;
import com.siurbinfo.srs.exception.NotHasSendEmailException;
import com.siurbinfo.srs.repository.config.ConfigRepository;
import com.siurbinfo.srs.repository.email.OutBoxEmailRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SmtpServiceTest {

    private static final String URL = "https://srs.example.com/";
    private static final String FROM = "srs@example.com";
    private static final String CAIXA_RESERVA = "reserva@example.com";
    private static final String CAIXA_FROTA = "frota@example.com";
    private static final String SOLICITANTE = "fulano@prefeitura.sp.gov.br";

    private static final Long ID_USUARIO = 7L;

    private static final Map<ReserveStatus, String> TITULO_POR_STATUS = new EnumMap<>(Map.of(
            ReserveStatus.ENVIADA_PARA_ANALISE, "Solicitação recebida. Protocolo 000123.",
            ReserveStatus.APROVADA,             "Reserva aprovada. Protocolo 000123.",
            ReserveStatus.REPROVADA,            "Solicitação reprovada. Protocolo 000123.",
            ReserveStatus.CANCELADA,            "Reserva cancelada. Protocolo 000123.",
            ReserveStatus.FINALIZADA,           "Reserva finalizada. Protocolo 000123."));

    private static final Map<ReserveStatus, String> ASSUNTO_POR_STATUS = new EnumMap<>(Map.of(
            ReserveStatus.ENVIADA_PARA_ANALISE, "SOLICITAÇÃO ENVIADA",
            ReserveStatus.APROVADA,             "SOLICITAÇÃO APROVADA",
            ReserveStatus.REPROVADA,            "SOLICITAÇÃO REPROVADA",
            ReserveStatus.CANCELADA,            "SOLICITAÇÃO CANCELADA",
            ReserveStatus.FINALIZADA,           "RESERVA FINALIZADA"));

    private static final Map<ReserveStatus, String> SAUDACAO_POR_STATUS = new EnumMap<>(Map.of(
            ReserveStatus.ENVIADA_PARA_ANALISE, "Fulano de Tal",
            ReserveStatus.APROVADA,             "Fulano de Tal",
            ReserveStatus.REPROVADA,            "Fulano de Tal",
            ReserveStatus.CANCELADA,            "Beltrano de Tal",
            ReserveStatus.FINALIZADA,           "Fulano de Tal"));

    private static final List<ReserveStatus> COM_COPIA_PRA_CAIXA =
            List.of(ReserveStatus.ENVIADA_PARA_ANALISE, ReserveStatus.FINALIZADA);

    @Test
    void sendEmail_renderizaOTemplateDeSolicitacaoRecebida() {
        noCenario(cenario -> {
            cenario.novoServico().sendEmail(auditorium(ReserveStatus.ENVIADA_PARA_ANALISE));
            String html = cenario.outbox().getFirst().getHtml();

            assertTrue(html.contains("Fulano de Tal"), "o html nao tem o username");
            assertTrue(html.contains("Reserva de auditorio"), "o html nao tem a finalidade");
            assertTrue(html.contains("000123"), "o html nao tem o protocolo");
            assertTrue(html.contains(URL), "o html nao tem o urlAccompany");
        });
    }

    @Test
    void sendEmail_saudacaoSobreviveAoTextoDoUsername() {
        noCenario(cenario -> {
            cenario.novoServico().sendEmail(auditorium(ReserveStatus.ENVIADA_PARA_ANALISE));
            String html = cenario.outbox().getFirst().getHtml();

            assertTrue(html.contains("Olá,"), "a saudacao sumiu do paragrafo");
            assertTrue(html.contains(">Fulano de Tal</strong>"),
                    "o nome tem que sair em negrito dentro da saudacao");
        });
    }

    @Test
    void sendEmail_cadaStatusCaiNoSeuTemplate() {
        noCenario(cenario -> {
            for (ReserveStatus status : ReserveStatus.values()) {
                cenario.novoServico().sendEmail(auditorium(status));
                EmailEntity email = cenario.outbox().getFirst();

                assertEquals(ASSUNTO_POR_STATUS.get(status), email.getSubject(),
                        status + " saiu com o assunto errado");
                assertTrue(email.getHtml().contains(TITULO_POR_STATUS.get(status)),
                        status + " nao renderizou o template esperado");
                assertTrue(email.getHtml().contains(">" + SAUDACAO_POR_STATUS.get(status) + "</strong>"),
                        status + " saiu com a saudacao errada");
                assertTrue(email.getHtml().contains("000123"), status + " saiu sem o protocolo");
            }
        });
    }

    @Test
    void sendEmail_salaDeReuniao_cadaStatusCaiNoSeuTemplate() {
        noCenario(cenario -> {
            for (ReserveStatus status : ReserveStatus.values()) {
                cenario.novoServico().sendEmail(meetingRoom(status));
                EmailEntity email = cenario.outbox().getFirst();

                assertEquals(ASSUNTO_POR_STATUS.get(status), email.getSubject(),
                        status + " (sala) saiu com o assunto errado");
                assertTrue(email.getHtml().contains(TITULO_POR_STATUS.get(status)),
                        status + " (sala) nao renderizou o template esperado");
            }
        });
    }

    @Test
    void sendEmail_veiculo_cadaStatusCaiNoSeuTemplate() {
        noCenario(cenario -> {
            for (ReserveStatus status : ReserveStatus.values()) {
                cenario.novoServico().sendEmail(fleetVehicle(status));
                EmailEntity email = cenario.outbox().getFirst();

                assertEquals(ASSUNTO_POR_STATUS.get(status), email.getSubject(),
                        status + " (veiculo) saiu com o assunto errado");
                assertTrue(email.getHtml().contains(TITULO_POR_STATUS.get(status)),
                        status + " (veiculo) nao renderizou o template esperado");
            }
        });
    }

    @Test
    void sendEmail_salaDeReuniao_mostraOAndarQueIdentificaASala() {
        noCenario(cenario -> {
            cenario.novoServico().sendEmail(meetingRoom(ReserveStatus.APROVADA));

            assertTrue(cenario.outbox().getFirst().getHtml().contains("12º andar"),
                    "o html da sala nao tem o andar");
        });
    }

    @Test
    void sendEmail_veiculo_mostraModeloPlacaEMotorista() {
        noCenario(cenario -> {
            cenario.novoServico().sendEmail(fleetVehicle(ReserveStatus.APROVADA));
            String html = cenario.outbox().getFirst().getHtml();

            assertTrue(html.contains("Fiat Doblo"), "o html do veiculo nao tem o modelo");
            assertTrue(html.contains("ABC1D23"), "o html do veiculo nao tem a placa");
            assertTrue(html.contains("Motorista de Tal"), "o html do veiculo nao tem o motorista");
        });
    }

    @Test
    void sendEmail_auditorio_motivoDaAberturaSaiEmCampoProprio() {
        noCenario(cenario -> {
            AuditoriumEntity e = auditorium(ReserveStatus.APROVADA);
            e.setTimeOpenAuditorium(LocalTime.of(8, 0));
            e.setReasonOpenAuditorium("Montagem do palco e passagem de som");
            e.setObservation("Levar cadeiras extras");

            cenario.novoServico().sendEmail(e);
            String html = cenario.outbox().getFirst().getHtml();

            assertTrue(html.contains("Motivo da abertura"),
                    "o html do auditorio nao tem o rotulo do campo novo");
            assertTrue(html.contains("Montagem do palco e passagem de som"),
                    "o html do auditorio nao tem o motivo da abertura");
            assertTrue(html.contains("Observações") && html.contains("Levar cadeiras extras"),
                    "as observacoes tinham que continuar saindo, em bloco separado");
        });
    }

    @Test
    void sendEmail_solicitacaoEFinalizacao_copiamACaixaResponsavel() {
        noCenario(cenario -> {
            for (ReserveStatus status : ReserveStatus.values()) {
                cenario.novoServico().sendEmail(auditorium(status));
                List<String> auditorio = destinatarios(cenario.outbox());

                cenario.novoServico().sendEmail(fleetVehicle(status));
                List<String> veiculo = destinatarios(cenario.outbox());

                if (COM_COPIA_PRA_CAIXA.contains(status)) {
                    assertEquals(List.of(SOLICITANTE, CAIXA_RESERVA), auditorio,
                            status + " (auditorio) nao copiou a caixa de reservas");
                    assertEquals(List.of(SOLICITANTE, CAIXA_FROTA), veiculo,
                            status + " (veiculo) nao copiou a caixa da frota");
                } else if (status == ReserveStatus.APROVADA) {
                    assertEquals(List.of(SOLICITANTE, CAIXA_RESERVA), auditorio,
                            status + " (auditorio) nao mandou o coffee break pra caixa de reservas");
                    assertEquals(List.of(SOLICITANTE), veiculo,
                            status + " (veiculo) tinha que ir so pro solicitante");
                } else {
                    assertEquals(List.of(SOLICITANTE), auditorio,
                            status + " (auditorio) tinha que ir so pro solicitante");
                    assertEquals(List.of(SOLICITANTE), veiculo,
                            status + " (veiculo) tinha que ir so pro solicitante");
                }
            }
        });
    }

    @Test
    void sendEmail_destinatarioSaiDoIdDoUsuarioDonoDaReserva() {
        noCenario(cenario -> {
            cenario.novoServico().sendEmail(auditorium(ReserveStatus.APROVADA));

            verify(cenario.userRepository).getEmailById(ID_USUARIO);
        });
    }

    @Test
    void sendEmail_gravaNaOutboxPendenteSemTentativas() {
        noCenario(cenario -> {
            cenario.novoServico().sendEmail(auditorium(ReserveStatus.APROVADA));
            EmailEntity email = cenario.outbox().getFirst();

            assertEquals(EmailStatus.PENDING, email.getStatus());
            assertEquals(0, email.getAttempts());
            assertNotNull(email.getNextAttemptAt(), "sem next_attempt_at a linha nunca e lida");
        });
    }

    @Test
    void sendEmail_idempotencyKeyIdentificaReservaStatusEDestinatario() {
        noCenario(cenario -> {
            cenario.novoServico().sendEmail(auditorium(ReserveStatus.ENVIADA_PARA_ANALISE));

            assertEquals(List.of(
                            "123:AUDITORIUM:ENVIADA_PARA_ANALISE:" + SOLICITANTE,
                            "123:AUDITORIUM:ENVIADA_PARA_ANALISE:" + CAIXA_RESERVA),
                    cenario.outbox().stream().map(EmailEntity::getIdempotencyKey).toList());
        });
    }

    @Test
    void sendEmail_naoFalaComOServidorSmtp() {
        noCenario(cenario -> {
            cenario.novoServico().sendEmail(auditorium(ReserveStatus.APROVADA));

            verifyNoInteractions(cenario.mailSender);
        });
    }

    @Test
    void sendEmail_auditorioComCafeAprovado_mandaOPedidoPraCaixaDeReservas() {
        noCenario(cenario -> {
            AuditoriumEntity e = auditorium(ReserveStatus.APROVADA);
            e.setFoodSetupTime(LocalTime.of(8, 30));
            e.setServiceTime(LocalTime.of(9, 0));

            cenario.novoServico().sendEmail(e);
            List<EmailEntity> outbox = cenario.outbox();

            assertEquals(2, outbox.size(), "a aprovacao tinha que gerar o e-mail do solicitante e o pedido");
            EmailEntity pedido = outbox.get(1);
            assertEquals(CAIXA_RESERVA, pedido.getRecipient());
            assertEquals("SOLICITAÇÃO COFFEE BREAK", pedido.getSubject());

            String html = pedido.getHtml();
            assertTrue(html.contains("Solicitação de coffee break. Reserva de auditorio"),
                    "nao renderizou o template de coffee break");
            assertFalse(html.contains("Tipo de coffee break"), "o tipo do coffee break saiu do pedido");
            assertTrue(html.contains("120"), "o html nao tem o numero de pessoas");
            assertTrue(html.contains("10/09/2026"), "o html nao tem a data do evento");
            assertTrue(html.contains("08:30"), "o html nao tem o horario de montagem");
            assertTrue(html.contains("09:00"), "o html nao tem o horario do servico");
            assertTrue(html.contains("Rua XV de Novembro, 165"), "o html nao tem o endereco");
            assertTrue(html.contains("mailto:" + CAIXA_RESERVA), "o rodape saiu sem o e-mail de suporte");
        });
    }

    @Test
    void sendEmail_coffeeBreak_idempotencyKeyNaoColideComADaAprovacao() {
        noCenario(cenario -> {
            cenario.novoServico().sendEmail(auditorium(ReserveStatus.APROVADA));

            assertEquals(List.of(
                            "123:AUDITORIUM:APROVADA:" + SOLICITANTE,
                            "123:COFFEE_BREAK:APROVADA:" + CAIXA_RESERVA),
                    cenario.outbox().stream().map(EmailEntity::getIdempotencyKey).toList());
        });
    }

    @Test
    void sendEmail_auditorioSemCafeAprovado_naoMandaOPedido() {
        noCenario(cenario -> {
            Map<String, AuditoriumEntity> casos = Map.of(
                    "cafe nao pedido", semCafe(),
                    "cafe reprovado", comCafe(Boolean.FALSE),
                    "cafe em analise", comCafe(null));

            casos.forEach((caso, entidade) -> {
                cenario.novoServico().sendEmail(entidade);

                assertEquals(List.of(SOLICITANTE), destinatarios(cenario.outbox()),
                        caso + ": so o e-mail de aprovacao tinha que sair");
            });
        });
    }

    private AuditoriumEntity semCafe() {
        AuditoriumEntity e = auditorium(ReserveStatus.APROVADA);
        e.setNecessaryCoffee(false);
        e.setIsApproveCoffeeBreak(null);
        return e;
    }

    private AuditoriumEntity comCafe(Boolean aprovado) {
        AuditoriumEntity e = auditorium(ReserveStatus.APROVADA);
        e.setIsApproveCoffeeBreak(aprovado);
        return e;
    }

    @Test
    void send_entregaOHtmlProDestinatarioInformado() {
        noCenario(cenario -> {
            SmtpService service = cenario.novoServico();

            service.send(SOLICITANTE, "SOLICITAÇÃO APROVADA", "<html><body>corpo</body></html>");

            MimeMessage enviada = cenario.mensagemEnviada();
            assertEquals(SOLICITANTE, primeiroDestinatario(enviada),
                    "o e-mail foi parar em outro endereco que nao o recebido em send(to, ...)");
            assertEquals("SOLICITAÇÃO APROVADA", assuntoDe(enviada));
            assertTrue(corpoHtmlDe(enviada).contains("corpo"), "o html nao chegou na mensagem");
        });
    }

    @Test
    void send_falhaNoSmtp_viraNotHasSendEmail() {
        noCenario(cenario -> {
            SmtpService service = cenario.novoServico();
            doThrow(new MailSendException("smtp fora do ar"))
                    .when(cenario.mailSender).send(any(MimeMessage.class));

            assertThrows(NotHasSendEmailException.class,
                    () -> service.send(SOLICITANTE, "assunto", "<html></html>"));
        });
    }

    @Test
    void getIsActiveSmtp_devolveOQueEstaGravadoNaConfig() {
        Config config = new Config();
        when(config.repository.getLastModificationSmtp()).thenReturn(false);

        assertFalse(config.service.getIsActiveSmtp());
    }

    @Test
    void setIsActiveSmtp_desligaEGuardaQuemMudouEQuando() {
        Config config = new Config();
        ConfigEntity linha = config.linhaSemeada(true);
        when(config.cognitoUserService.getName("token-do-admin")).thenReturn("Admin de Tal");

        Instant antes = Instant.now();
        config.service.setIsActiveSmtp(jwt("token-do-admin"), false);

        verify(config.repository).save(linha);
        assertFalse(linha.isActiveSmt());
        assertEquals("Admin de Tal", linha.getUpdatedBy(),
                "o nome tem que vir do token de quem fez a alteracao");
        assertNotNull(linha.getUpdatedAt());
        assertTrue(Duration.between(antes, linha.getUpdatedAt()).abs().getSeconds() < 5,
                "updated_at tinha que ser o momento da alteracao, foi " + linha.getUpdatedAt());
    }

    @Test
    void setIsActiveSmtp_religa() {
        Config config = new Config();
        ConfigEntity linha = config.linhaSemeada(false);

        config.service.setIsActiveSmtp(jwt("token-do-admin"), true);

        verify(config.repository).save(linha);
        assertTrue(linha.isActiveSmt());
    }

    @Test
    void setIsActiveSmtp_semJwt_naoMexeNaConfig() {
        Config config = new Config();

        assertThrows(SecurityException.class, () -> config.service.setIsActiveSmtp(null, false));

        verify(config.repository, never()).save(any());
    }

    @Test
    void setIsActiveSmtp_semALinhaDeConfig_estouraEntityNotFound() {
        Config config = new Config();
        when(config.repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> config.service.setIsActiveSmtp(jwt("token-do-admin"), false));

        verify(config.repository, never()).save(any());
    }

    private static final class Config {

        private final ConfigRepository repository = mock(ConfigRepository.class);
        private final CognitoUserService cognitoUserService = mock(CognitoUserService.class);
        private final SmtpService service = new SmtpService(new ReservationMailModelFactory(),
                mock(SpringTemplateEngine.class), mock(JavaMailSender.class),
                mock(OutBoxEmailRepository.class), mock(UserRepository.class),
                repository, cognitoUserService);

        ConfigEntity linhaSemeada(boolean ativo) {
            ConfigEntity linha = new ConfigEntity();
            linha.setId(1L);
            linha.setActiveSmt(ativo);
            linha.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
            when(repository.findById(1L)).thenReturn(Optional.of(linha));
            return linha;
        }
    }

    private static Jwt jwt(String token) {
        return Jwt.withTokenValue(token).header("alg", "none").claim("sub", "admin").build();
    }

    private UserEntity usuario() {
        UserEntity user = new UserEntity();
        user.setId(ID_USUARIO);
        user.setEmail(SOLICITANTE);
        return user;
    }

    private AuditoriumEntity auditorium(ReserveStatus status) {
        AuditoriumEntity e = new AuditoriumEntity();
        e.setId(123L);
        e.setUser(usuario());
        e.setReservationType(ReserveType.AUDITORIUM);
        e.setStatus(status);
        e.setReservationDate(LocalDate.of(2026, 9, 10));
        e.setStartTime(LocalTime.of(9, 0));
        e.setEndTime(LocalTime.of(11, 0));
        e.setReason("Reserva de auditorio");
        e.setUserNameRequester("Fulano de Tal");
        e.setUserNameApprover("Beltrano de Tal");
        e.setRequesterDepartment("SIURB");
        e.setNumPeople(120);
        e.setNecessaryScreen(true);
        e.setNecessarySoundSystem(false);
        e.setNecessaryCoffee(true);
        e.setReasonCoffee("Café para 120 pessoas");
        e.setIsApproveCoffeeBreak(Boolean.TRUE);
        e.setDateCreate(Instant.now());
        e.setDateAnalysis(Instant.now());
        e.setReasonFailure("Auditório em manutenção.");
        return e;
    }

    private MeetingRoomEntity meetingRoom(ReserveStatus status) {
        RoomEntity room = new RoomEntity();
        room.setId(4L);
        room.setFloor("12º andar");

        MeetingRoomEntity e = new MeetingRoomEntity();
        e.setId(123L);
        e.setUser(usuario());
        e.setRoom(room);
        e.setReservationType(ReserveType.MEETING_ROOM);
        e.setStatus(status);
        e.setReservationDate(LocalDate.of(2026, 9, 10));
        e.setStartTime(LocalTime.of(14, 0));
        e.setEndTime(LocalTime.of(15, 30));
        e.setUserNameRequester("Fulano de Tal");
        e.setUserNameApprover("Beltrano de Tal");
        e.setRequesterDepartment("SIURB");
        e.setNumPeople(8);
        e.setIsCoffee(Boolean.TRUE);
        e.setReasonCoffee("Reunião com a diretoria");
        e.setIsApproveCoffee(Boolean.TRUE);
        e.setDateCreate(Instant.now());
        e.setDateAnalysis(Instant.now());
        e.setReasonFailure("Sala ocupada.");
        return e;
    }

    private FleetVehicleEntity fleetVehicle(ReserveStatus status) {
        DriverEntity driver = new DriverEntity();
        driver.setName("Motorista de Tal");
        driver.setModelCar("Fiat Doblo");
        driver.setLicensePlate("ABC1D23");

        FleetVehicleEntity e = new FleetVehicleEntity();
        e.setId(123L);
        e.setUser(usuario());
        e.setDriver(driver);
        e.setReservationType(ReserveType.FLEET_VEHICLE);
        e.setStatus(status);
        e.setReservationDate(LocalDate.of(2026, 9, 10));
        e.setExitTime(LocalTime.of(8, 0));
        e.setNecessaryReturn(true);
        e.setReturnTime(LocalTime.of(17, 0));
        e.setReason("Vistoria de obra");
        e.setUserNameRequester("Fulano de Tal");
        e.setUserNameApprover("Beltrano de Tal");
        e.setRequesterDepartment("SIURB");
        e.setDepartureAddress("Rua Libero Badaró, 425");
        e.setDestinationAddress("Av. Paulista, 1000");
        e.setTypeVehicle(VehicleType.CARRO);
        e.setQuantityPassengers(3);
        e.setDateCreate(Instant.now());
        e.setDateAnalysis(Instant.now());
        e.setReasonFailure("Frota indisponível.");
        return e;
    }

    private void noCenario(Consumer<Cenario> teste) {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ThymeleafAutoConfiguration.class))
                .run(context -> teste.accept(new Cenario(context.getBean(SpringTemplateEngine.class))));
    }

    private static final class Cenario {

        private final SpringTemplateEngine engine;

        private OutBoxEmailRepository outbox;
        private UserRepository userRepository;
        private JavaMailSender mailSender;
        private ConfigRepository configRepository;
        private CognitoUserService cognitoUserService;

        private Cenario(SpringTemplateEngine engine) {
            this.engine = engine;
        }

        SmtpService novoServico() {
            outbox = mock(OutBoxEmailRepository.class);
            mailSender = mock(JavaMailSender.class);
            when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
            userRepository = mock(UserRepository.class);
            when(userRepository.getEmailById(ID_USUARIO)).thenReturn(SOLICITANTE);
            configRepository = mock(ConfigRepository.class);
            cognitoUserService = mock(CognitoUserService.class);

            SmtpService service = new SmtpService(new ReservationMailModelFactory(), engine,
                    mailSender, outbox, userRepository, configRepository, cognitoUserService);
            ReflectionTestUtils.setField(service, "from", FROM);
            ReflectionTestUtils.setField(service, "url", URL);
            ReflectionTestUtils.setField(service, "emailReserva", CAIXA_RESERVA);
            ReflectionTestUtils.setField(service, "emailFrota", CAIXA_FROTA);
            return service;
        }

        List<EmailEntity> outbox() {
            ArgumentCaptor<EmailEntity> captor = ArgumentCaptor.captor();
            verify(outbox, atLeastOnce()).save(captor.capture());
            return captor.getAllValues();
        }

        MimeMessage mensagemEnviada() {
            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.captor();
            verify(mailSender).send(captor.capture());
            try {
                captor.getValue().saveChanges();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return captor.getValue();
        }
    }

    private List<String> destinatarios(List<EmailEntity> emails) {
        return emails.stream().map(EmailEntity::getRecipient).toList();
    }

    private String primeiroDestinatario(MimeMessage message) {
        try {
            return message.getAllRecipients()[0].toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String assuntoDe(MimeMessage message) {
        try {
            return message.getSubject();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String corpoHtmlDe(MimeMessage message) {
        try {
            String html = corpoHtml(message);
            assertNotNull(html, "a mensagem saiu sem nenhuma parte text/html");
            return html;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String corpoHtml(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String html = corpoHtml(multipart.getBodyPart(i));
                if (html != null) {
                    return html;
                }
            }
        }
        return null;
    }
}
