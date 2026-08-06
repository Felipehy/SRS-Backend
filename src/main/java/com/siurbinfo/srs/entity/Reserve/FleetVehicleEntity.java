package com.siurbinfo.srs.entity.Reserve; // ajuste pro package real do seu projeto

import com.siurbinfo.srs.entity.DriverEntity;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// Representa uma solicitacao de reserva de veiculo da frota (tbl_fleet_vehicles),
// incluindo trajeto de ida, retorno opcional e motorista designado.
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
    private LocalDateTime dateAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type", nullable = false, length = 30)
    private ReserveType reservationType;

    @Column(name = "exit_time", nullable = false)
    private LocalTime exitTime;

    @Column(name = "reason", nullable = false)
    private String reason;

    // Motivo preenchido quando a reserva e reprovada
    @Column(name = "reason_failure")
    private String reasonFailure;

    @Column(name = "departure_address", nullable = false)
    private String departureAddress;

    @Column(name = "destination_address", nullable = false)
    private String destinationAddress;

    // Motorista designado para conduzir o veiculo nesta reserva
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_driver")
    private DriverEntity driver;

    @Column(name = "user_name_requester", nullable = false, length = 255)
    private String userNameRequester;

    @Column(name = "user_name_approver", length = 255)
    private String userNameApprover;

    // Indica se havera trajeto de volta (retorno); se true, os campos abaixo se aplicam
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

    // Indica se e necessario um veiculo do tipo van (ex: por causa da quantidade de passageiros)
    @Column(name = "is_necessary_van", nullable = false)
    private boolean necessaryVan;

    @Column(name = "reason_use_van")
    private String reasonUseVan;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_vehicle", nullable = false, length = 30)
    private VehicleType typeVehicle;

    @Column(name = "quantity_passengers", nullable = false)
    private Integer quantityPassengers;

    // Status atual do fluxo de aprovacao da reserva
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReserveStatus status;

    @CreationTimestamp
    @Column(name = "date_create", nullable = false, updatable = false)
    private LocalDateTime dateCreate;
}