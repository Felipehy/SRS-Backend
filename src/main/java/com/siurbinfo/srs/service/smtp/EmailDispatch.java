package com.siurbinfo.srs.service.smtp;

import com.siurbinfo.srs.entity.email.EmailEntity;
import com.siurbinfo.srs.enums.EmailStatus;
import com.siurbinfo.srs.exception.NotHasSendEmailException;
import com.siurbinfo.srs.repository.email.OutBoxEmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

// Worker da outbox: le as linhas pendentes e entrega. Ligado por padrao; o perfil de
// teste desliga (app.mail.dispatch.enabled=false) pra nenhuma suite tocar no SMTP nem
// mexer no status das linhas de outbox_email do banco de teste.
@Component
@ConditionalOnProperty(name = "app.mail.dispatch.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class EmailDispatch {

    private final OutBoxEmailRepository outBoxEmailRepository;
    private final SmtpService smtpService;

    @Scheduled(fixedDelay = 15_000)
    @Transactional
    public void dispatch(){
        if (!smtpService.getIsActiveSmtp()) return;
        for (EmailEntity email: outBoxEmailRepository.getOutBoxEmail()){
            try{
                smtpService.send(email.getRecipient(), email.getSubject(), email.getHtml());
                email.setStatus(EmailStatus.SENT);
            } catch (MailException | NotHasSendEmailException e){
                email.setAttempts(email.getAttempts()+1);
                email.setLastError(e.getMessage());
                if (email.getAttempts() >= 5) email.setStatus(EmailStatus.DEAD);
                else email.setNextAttemptAt(Instant.now().plus(
                        Duration.ofMinutes((long) Math.pow(2, email.getAttempts()))));
            }
        }
    }

}
