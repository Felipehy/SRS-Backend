package com.siurbinfo.srs.entity.Reserve;

import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Representa uma solicitacao de reserva de auditorio (tbl_auditorium), com dados do
// pedido, aprovacao e infraestrutura necessaria para o evento.
@Entity
@Table(name = "tbl_auditorium")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class AuditoriumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "`reservation_date`", nullable = false)
    private LocalDate reservationDate;

    // Data/hora em que a solicitacao foi analisada (aprovada/reprovada)
    @Column(name = "date_analysis")
    private LocalDateTime dateAnalysis;

    // Horario em que o auditorio deve ser aberto (pode ser antes do start_time)
    @Column(name = "time_open_auditory")
    private LocalTime timeOpenAuditorium;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type", nullable = false, length = 30)
    private ReserveType reservationType;

    @Column(name = "reason", nullable = false)
    private String reason;

    // Motivo preenchido quando a reserva e reprovada
    @Column(name = "reason_failure")
    private String reasonFailure;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "user_name_requester", length = 255, nullable = false)
    private String userNameRequester;

    @Column(name = "user_name_approver", length = 255)
    private String userNameApprover;

    @Column(name = "num_people", nullable = false)
    private Integer numPeople;

    @Column(name = "is_external_public", nullable = false)
    private boolean externalPublic;

    @Column(name = "is_necessary_screen", nullable = false)
    private boolean necessaryScreen;

    @Column(name = "is_necessary_sound_system", nullable = false)
    private boolean necessarySoundSystem;

    @Column(name = "is_necessary_coffee", nullable = false)
    private boolean necessaryCoffee;

    @Column(name = "observation", length = 1000)
    private String observation;

    // Status atual do fluxo de aprovacao da reserva
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReserveStatus status;

    @CreationTimestamp
    @Column(name = "date_create", nullable = false, updatable = false)
    private LocalDateTime dateCreate;
}