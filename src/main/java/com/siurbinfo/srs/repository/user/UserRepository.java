package com.siurbinfo.srs.repository.user;

import com.siurbinfo.srs.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository JPA para persistencia de UserEntity. Sem queries customizadas (apenas CRUD padrao).
 */
public interface UserRepository extends JpaRepository<UserEntity,Long> {

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE tbl_user
    SET img = :value
    WHERE email = :email
    """, nativeQuery = true)
    void setImgInUser(@Param("value") String value, @Param("email") String email);

    @Query(value = """
    SELECT tu.img
    FROM tbl_user tu
    WHERE tu.email = :email
    """, nativeQuery = true)
    String getImgInUser(@Param("email") String email);

    void deleteByEmail(String email);

}
