package com.siurbinfo.srs.repository.log;

import com.siurbinfo.srs.entity.log.LogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LogRepository extends JpaRepository<LogEntity, Long> {

    @Query(value = """
        select
           tl.id                as id,
           tl.reservation_type  as reservationType,
           tl.modification      as modification,
           tl.local_modification as localModification,
           tu.given_name        as givenName,
           tu.family_name       as familyName,
           tu.email             as email,
           tl.ip                as ip,
           tl.group_user        as groupUser,
           tl.type_log          as typeLog,
           tl.modification_date as modificationDate,
           tl.id_data           as idData
       from tbl_logs tl
       left join tbl_user tu on tl.id_user = tu.id
       order by tl.modification_date desc
       limit 10 offset :offSet
    """, nativeQuery = true)
    List<LogProjection> getLogs(@Param("offSet") Long offSet);

}