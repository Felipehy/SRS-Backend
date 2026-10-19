package com.siurbinfo.srs.repository.driver;

import com.siurbinfo.srs.entity.driver.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<DriverEntity, Long> {
}
