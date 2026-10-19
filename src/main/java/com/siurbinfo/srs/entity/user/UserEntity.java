package com.siurbinfo.srs.entity.user;

import com.siurbinfo.srs.entity.department.DepartmentEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "tbl_user")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    @Column(name = "email",unique = true, nullable = false)
    private String email;

    @Column(name = "img")
    private String linkImg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_department")
    @ToString.Exclude
    private DepartmentEntity department;

    // o Hibernate manda a coluna no insert mesmo nula: o default precisa estar aqui (V35)
    @Column(name = "is_horizontal_navbar")
    private Boolean isHorizontalNavbar = false;

    @Column(name = "color_auditorium", length = 7)
    private String colorAuditorium;

    @Column(name = "color_fleet_vehicles", length = 7)
    private String colorFleetVehicles;

    @Column(name = "color_meeting_room", length = 7)
    private String colorMeetingRoom;

    @Column(name = "given_name")
    private String givenName;

    @Column(name = "family_name")
    private String familyName;

    // not null no banco (V38): sem esse default o insert de usuario novo quebra
    @Column(name = "first_login")
    private Boolean isFirstLogin = true;
}
