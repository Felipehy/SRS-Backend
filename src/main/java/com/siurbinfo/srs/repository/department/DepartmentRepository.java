package com.siurbinfo.srs.repository.department;

import com.siurbinfo.srs.entity.department.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity,Long> {

    @Query(value = """
    insert into tbl_department
    (name,id_room) values (:name,:idRoom)
    """,nativeQuery = true)
    void createDepartment(@Param("name") String name, @Param("idRoom") Long idRoom);

    @Modifying
    @Query(value = """
    update tbl_department
    set name =:name, id_room =:idRoom
    where id =:id
    """, nativeQuery = true)
    void updateDepartment(@Param("id") Long id,@Param("name") String name, @Param("idRoom") Long idRoom);

    // join fetch evita N+1 ao montar o DepartmentResponseDTO (que le room.floor)
    @Query("""
    select d
    from DepartmentEntity d
    left join fetch d.room r
    order by d.id
    """)
    List<DepartmentEntity> getAllDepartment();

}
