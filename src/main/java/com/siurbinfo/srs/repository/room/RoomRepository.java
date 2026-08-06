package com.siurbinfo.srs.repository.room;

import com.siurbinfo.srs.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository JPA para persistencia de RoomEntity. Sem queries customizadas (apenas CRUD padrao).
 */
public interface RoomRepository extends JpaRepository<RoomEntity, Long> {
}
