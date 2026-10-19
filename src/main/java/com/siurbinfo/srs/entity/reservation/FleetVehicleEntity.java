package com.siurbinfo.srs.entity.reservation;

import com.siurbinfo.srs.entity.driver.DriverEntity;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveStatus;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.enums.VehicleType;
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
@Table(name = "tbl_fleet_vehicles")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FleetVehicleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "date_analysis")
    private Instant dateAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type", nullable = false, length = 30)
    private ReserveType reservationType;

    @Column(name = "exit_time", nullable = false)
    private LocalTime exitTime;

    @Column(name = "reason", nullable = false)
    private String reason;

    // so preenchido quando a reserva e reprovada
    @Column(name = "reason_failure")
    private String reasonFailure;

    @Column(name = "departure_address", nullable = false)
    private String departureAddress;

    @Column(name = "destination_address", nullable = false)
    private String destinationAddress;

    // Inicializado: a coluna e NOT NULL (V37) e o campo e Boolean, entao quem monta a
    // entidade sem passar pelo mapper deixaria null e quebraria no insert. Os demais
    // booleanos daqui sao primitivos e ja nascem false por conta disso.
    @Column(name = "is_outside_the_municipality", nullable = false)
    private Boolean isOutsideTheMunicipality = false;

    @Column(name = "reason_outside_the_municipality", length = 300)
    private String reasonOutsideTheMunicipality;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_driver")
    private DriverEntity driver;

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

    // se true, os 3 campos de retorno abaixo valem
    @Column(name = "is_necessary_return", nullable = false)
    private boolean necessaryReturn;

    @Column(name = "return_time")
    private LocalTime returnTime;

    @Column(name = "departure_address_return")
    private String departureAddressReturn;

    @Column(name = "destination_address_return")
    private String destinationAddressReturn;

    @Column(name = "contact", nullable = false, length = 50)
    private String contact;

    @Column(name = "is_necessary_van", nullable = false)
    private boolean necessaryVan;

    @Column(name = "reason_use_van")
    private String reasonUseVan;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_vehicle", nullable = false, length = 30)
    private VehicleType typeVehicle;

    @Column(name = "quantity_passengers", nullable = false)
    private Integer quantityPassengers;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReserveStatus status;

    @CreationTimestamp
    @Column(name = "date_create", nullable = false, updatable = false)
    private Instant dateCreate;
}