package com.siurbinfo.srs.entity.email;

import com.siurbinfo.srs.enums.EmailStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

// Outbox de e-mail (outbox_email): a linha e gravada na mesma transacao da reserva e um
// worker le as pendentes depois. Se o SMTP cair, a reserva continua salva e o e-mail e
// reprocessado a partir daqui.
@Table(name = "outbox_email")
@Entity
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class EmailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    @Column(name = "recipient", nullable = false, length = 320)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "html")
    private String html;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailStatus status = EmailStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    // O UNIQUE no banco e o que impede envio duplicado em retry/reprocessamento.
    @Column(name = "idempotency_key", nullable = false, length = 200, unique = true)
    private String idempotencyKey;

}
