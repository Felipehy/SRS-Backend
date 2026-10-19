package com.siurbinfo.srs.entity.log;


import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.enums.ReserveType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "tbl_logs")
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class LogEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type")
    private ReserveType reservationType;

    @Column(name = "id_data")
    private Long idData;

    @Column(name = "modification", nullable = false)
    private String modification;

    @Column(name = "local_modification", nullable = false)
    private String localModification;

    @Column(name = "id_user")
    private Long user;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "group_user", nullable = false)
    private List<String> groupUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_log", nullable = false)
    private LogsType logsType;

    @CreationTimestamp
    @Column(name = "modification_date", nullable = false)
    private Instant modificationDate;
}
