package com.siurbinfo.srs.repository.driver;

import com.siurbinfo.srs.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA para persistencia de DriverEntity. Sem queries customizadas (apenas CRUD padrao).
 */
public interface DriverRepository extends JpaRepository<DriverEntity, Long> {
}
