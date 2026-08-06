package com.siurbinfo.srs.entity.Reserve; // ajuste pro package real do seu projeto

import com.siurbinfo.srs.entity.RoomEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Representa uma solicitacao de reserva de sala de reuniao (tbl_meeting_room),
// vinculada a uma sala fisica (RoomEntity).
@Entity
@Table(name = "tbl_meeting_room")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MeetingRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "date_analysis")
    private LocalDateTime dateAnalysis;

    // Sala fisica reservada
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_room", nullable = false)
    private RoomEntity room;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type", nullable = false, length = 30)
    private ReserveType reservationType;

    // Motivo preenchido quando a reserva e reprovada
    @Column(name = "reason_failure")
    private String reasonFailure;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "user_name_requester", nullable = false, length = 255)
    private String userNameRequester;

    @Column(name = "user_name_approver", length = 255)
    private String userNameApprover;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "num_people", nullable = false)
    private Integer numPeople;

    @Column(name = "is_coffee", nullable = false)
    private Boolean isCoffee;

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