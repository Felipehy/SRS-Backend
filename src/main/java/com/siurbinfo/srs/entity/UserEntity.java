package com.siurbinfo.srs.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Representa um usuario do sistema (tbl_user), identificado por email unico.
@Entity
@Table(name = "tbl_user")
@Getter
@Setter
@ToString
@EqualsAndHashCode
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

    // Sala/andar onde o usuario esta lotado
    @ManyToOne
    @JoinColumn(name = "id_room")
    private RoomEntity room;

}
