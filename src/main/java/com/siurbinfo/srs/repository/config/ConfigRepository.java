package com.siurbinfo.srs.repository.config;

import com.siurbinfo.srs.entity.config.ConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConfigRepository extends JpaRepository<ConfigEntity, Long> {

    @Query(value = """
        select active_smtp
        from tbl_config
        order by id desc
        limit 1
    """, nativeQuery = true)
    Boolean getLastModificationSmtp();
}
