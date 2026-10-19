package com.siurbinfo.srs.entity.reservation;

import com.siurbinfo.srs.entity.room.RoomEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

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
    private Instant dateAnalysis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_room", nullable = false)
    private RoomEntity room;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type", nullable = false, length = 30)
    private ReserveType reservationType;

    // so preenchido quando a reserva e reprovada
    @Column(name = "reason_failure")
    private String reasonFailure;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "reason_outside_hours",length = 300)
    private String reasonOutsideHours;

    @Column(name = "user_name_requester", nullable = false, length = 255)
    private String userNameRequester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user")
    @ToString.Exclude
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canceled_by")
    @ToString.Exclude
    private UserEntity canceledBy;

    @Column(name = "reasons_cancellation")
    private String reasonsCancellation;

    @Column(name = "date_cancellation")
    private Instant dateCancellation;

    @Column(name = "telephone", length = 15)
    private String telephone;

    @Column(name = "requester_department", length = 50)
    private String requesterDepartment;

    @Column(name = "user_name_approver", length = 255)
    private String userNameApprover;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "num_people", nullable = false)
    private Integer numPeople;

    @Column(name = "is_coffee")
    private Boolean isCoffee;

    @Column(name = "reason_coffee")
    private String reasonCoffee;

    @Column(name = "is_approve_coffee")
    private Boolean isApproveCoffee;

    @Column(name = "reason_reject_coffee")
    private String reasonRejectCoffee;

    @Column(name = "observation", length = 1000)
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReserveStatus status;

    @CreationTimestamp
    @Column(name = "date_create", nullable = false, updatable = false)
    private Instant dateCreate;
}