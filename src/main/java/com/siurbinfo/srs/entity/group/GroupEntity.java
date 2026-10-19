package com.siurbinfo.srs.entity.group;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "tbl_groups")
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class GroupEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "group_name", length = 50, nullable = false)
    private String groupName;

    @Column(name = "group_cognito", length = 50, nullable = false)
    private String groupCognito;

    @Column(name = "color_group", length = 7)
    private String colorGroup;

    @Column(name = "description", length = 150)
    private String description;

}
