package com.siurbinfo.srs.service.smtp;

import com.siurbinfo.srs.entity.config.ConfigEntity;
import com.siurbinfo.srs.entity.email.EmailEntity;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.enums.EmailStatus;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.EntityNotFoundException;
import com.siurbinfo.srs.exception.NotHasSendEmailException;
import com.siurbinfo.srs.exception.ReservationNotFoundedException;
import com.siurbinfo.srs.repository.config.ConfigRepository;
import com.siurbinfo.srs.repository.email.OutBoxEmailRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SmtpService {

    private static final Logger log = LoggerFactory.getLogger(SmtpService.class);

    // Tem que bater com o src="cid:logo" dos templates: o Content-ID e comparado
    // literalmente, entao "logo.png" aqui deixa a imagem quebrada no e-mail.
    private static final String LOGO_CONTENT_ID = "logo";
    private static final String LOGO_PATH = "static/img/Logo_SRS_completa.png";
    private final String NAME_FLEET = "SIURB - Frotas";
    private final String NAME_RESERVATION = "SIURB - Reservas";

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final ReservationMailModelFactory modelFactory;
    private final SpringTemplateEngine templateEngine;
    private final JavaMailSender mailSender;
    private final OutBoxEmailRepository outBoxEmailRepository;
    private final UserRepository userRepository;
    private final ConfigRepository configRepository;
    private final CognitoUserService cognitoUserService;

    @Value("${app.url}") private String url;
    @Value("${app.mail.frota}") private String emailFrota;
    @Value("${app.mail.reserva}") private String emailReserva;
    @Value("${spring.mail.username}") private String from;
    @Value("${app.mail.sender}") private String sender;

    public Boolean getIsActiveSmtp(){
        return configRepository.getLastModificationSmtp();
    }

    public void setIsActiveSmtp(Jwt jwt, Boolean isActive){
        if (jwt == null) {
            throw new SecurityException("Is necessary a json web token(jwt)");
        }
        ConfigEntity entity = configRepository.findById(1L).orElseThrow(() -> new EntityNotFoundException("Nenhuma configuração foi encontrada!"));
        Instant instant = Instant.now().atZone(ZoneId.of("America/Sao_Paulo")).toInstant();
        entity.setActiveSmt(isActive);
        entity.setUpdatedAt(instant);
        entity.setUpdatedBy(cognitoUserService.getName(jwt.getTokenValue()));
        configRepository.save(entity);
    }

    public void sendEmail(MeetingRoomEntity entity) {

        String email = userRepository.getEmailById(entity.getUser().getId());

        switch (entity.getStatus()) {
            case APROVADA -> sendEmails(List.of(email),
                    "SOLICITAÇÃO APROVADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_APPROVE,
                            modelFactory.approved(entity.getUserNameRequester(), url, emailReserva,NAME_RESERVATION, entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            case CANCELADA -> sendEmails(List.of(email),
                    "SOLICITAÇÃO CANCELADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_CANCEL,
                            modelFactory.cancelled(entity.getUserNameApprover(), url, emailReserva, entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            case REPROVADA -> sendEmails(List.of(email),
                    "SOLICITAÇÃO REPROVADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_REJECT,
                            modelFactory.rejected(entity.getUserNameRequester(), url, emailReserva,NAME_RESERVATION, entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            case FINALIZADA -> sendEmails(List.of(email, emailReserva),
                    "RESERVA FINALIZADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_FINISH,
                            modelFactory.finished(entity.getUserNameRequester(), url, emailReserva, entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            case ENVIADA_PARA_ANALISE -> sendEmails(List.of(email, emailReserva),
                    "SOLICITAÇÃO ENVIADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_REQUEST,
                            modelFactory.requested(entity.getUserNameRequester(), url, emailReserva, entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            default -> throw new IllegalArgumentException("Status not founded in entity");
        }
    }

    public void sendEmail(FleetVehicleEntity entity){

        String email = userRepository.getEmailById(entity.getUser().getId());

        switch (entity.getStatus()){
            case APROVADA -> sendEmails(List.of(email),
                    "SOLICITAÇÃO APROVADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_APPROVE,
                            modelFactory.approved(entity.getUserNameRequester(),url,emailFrota,NAME_FLEET,entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            case CANCELADA -> sendEmails(List.of(email),
                    "SOLICITAÇÃO CANCELADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_CANCEL,
                            modelFactory.cancelled(entity.getUserNameApprover(),url,emailFrota,entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            case REPROVADA -> sendEmails(List.of(email),
                    "SOLICITAÇÃO REPROVADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_REJECT,
                            modelFactory.rejected(entity.getUserNameRequester(),url,emailFrota,NAME_FLEET,entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            case FINALIZADA -> sendEmails(List.of(email,emailFrota),
                    "RESERVA FINALIZADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_FINISH,
                            modelFactory.finished(entity.getUserNameRequester(),url,emailFrota,entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            case ENVIADA_PARA_ANALISE -> sendEmails(List.of(email,emailFrota),
                    "SOLICITAÇÃO ENVIADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_REQUEST,
                            modelFactory.requested(entity.getUserNameRequester(),url,emailFrota,entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            default -> throw new IllegalArgumentException("Status not founded in entity");
        }
    }

    public void sendEmail(AuditoriumEntity entity){

        String email = userRepository.getEmailById(entity.getUser().getId());

        switch (entity.getStatus()){
            case APROVADA -> {
                sendEmails(List.of(email),
                        "SOLICITAÇÃO APROVADA",
                        templateHtml(ReservationMailModelFactory.TEMPLATE_APPROVE,
                                modelFactory.approved(entity.getUserNameRequester(),url,emailReserva,NAME_RESERVATION,entity)
                        ),
                        entity.getId(),
                        entity.getReservationType().name(),
                        entity.getStatus()
                );
                if (entity.isNecessaryCoffee() && Boolean.TRUE.equals(entity.getIsApproveCoffeeBreak())) {
                    sendEmails(List.of(emailReserva),
                            "SOLICITAÇÃO COFFEE BREAK",
                            templateHtml(ReservationMailModelFactory.TEMPLATE_COFFEE_BREAK,
                                    modelFactory.coffeeBreakApproved(entity, emailReserva)
                            ),
                            entity.getId(),
                            "COFFEE_BREAK",
                            entity.getStatus()
                    );
                }
            }
            case CANCELADA -> sendEmails(List.of(email),
                    "SOLICITAÇÃO CANCELADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_CANCEL,
                            modelFactory.cancelled(entity.getUserNameApprover(),url,emailReserva,entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            case REPROVADA -> sendEmails(List.of(email),
                    "SOLICITAÇÃO REPROVADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_REJECT,
                            modelFactory.rejected(entity.getUserNameRequester(),url,emailReserva,NAME_RESERVATION,entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            case FINALIZADA -> sendEmails(List.of(email,emailReserva),
                    "RESERVA FINALIZADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_FINISH,
                            modelFactory.finished(entity.getUserNameRequester(),url,emailReserva,entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            case ENVIADA_PARA_ANALISE -> sendEmails(List.of(email,emailReserva),
                    "SOLICITAÇÃO ENVIADA",
                    templateHtml(ReservationMailModelFactory.TEMPLATE_REQUEST,
                            modelFactory.requested(entity.getUserNameRequester(),url,emailReserva,entity)
                    ),
                    entity.getId(),
                    entity.getReservationType().name(),
                    entity.getStatus()
            );
            default -> throw new IllegalArgumentException("Status not founded in entity");
        }
    }

    private void sendEmails(List<String> emails, String subject, String html, Long reservationId, String type, ReserveStatus status){

        if (emails == null || emails.isEmpty()){throw new NotHasSendEmailException("Not founded any email");}

        for (String email: emails){
            outBoxEmailRepository.save(toEntity(email,subject,html,reservationId,type,status));
        }
    }

    private EmailEntity toEntity(String email, String subject, String html, Long reservationId, String type, ReserveStatus status){
        EmailEntity entity = new EmailEntity();
        entity.setHtml(html);
        entity.setSubject(subject);
        entity.setRecipient(email);
        entity.setIdempotencyKey("%s:%s:%s:%s".formatted(reservationId,type,status,email));
        return entity;
    }

    public void send(String to, String subject, String html){
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(msg, MimeMessageHelper.MULTIPART_MODE_MIXED, "UTF-8");
            helper.setFrom(from);
            helper.setTo(sender == null || sender.isBlank() ? to : sender);
            helper.setSubject(subject);
            helper.setText(html,true);
            helper.addInline(LOGO_CONTENT_ID, new ClassPathResource(LOGO_PATH));
            mailSender.send(msg);
            log.info("e-mail \"{}\" enviado para {}", subject, to);
        }catch (MessagingException | MailException e) {
            log.error("falha ao enviar o e-mail \"{}\" para {}", subject, to, e);
            throw new NotHasSendEmailException(e.getMessage());
        }
    }

    private String templateHtml(String template, Map<String, Object> variables){
        Context ctx = new Context(PT_BR);
        ctx.setVariables(variables);
        return templateEngine.process(template, ctx);
    }

}
