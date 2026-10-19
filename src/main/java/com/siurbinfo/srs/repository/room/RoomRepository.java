package com.siurbinfo.srs.repository.room;

import com.siurbinfo.srs.entity.room.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {

    @Modifying
    @Query(value = """
    update tbl_room
    set is_active =:isActive
    where id =:id
    """, nativeQuery = true)
    void setVisible(@Param("id") Long id, @Param("isActive") Boolean isActive);

    @Query(value = """
    select *
    from tbl_room tr
    order by id
    """,nativeQuery = true)
    List<RoomEntity> getRooms();

}
