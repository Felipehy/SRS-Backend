package com.siurbinfo.srs.repository.user;

import com.siurbinfo.srs.entity.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


/**
 * Desde a V39 a exclusao e logica (deleted_at) e o e-mail so e unico entre os ativos:
 * um e-mail excluido pode voltar como outra linha. Por isso toda query por e-mail
 * filtra deleted_at is null, senao as de resultado unico estouram com duas linhas.
 */
public interface UserRepository extends JpaRepository<UserEntity,Long> {

    @Modifying
    @Transactional
    @Query(value = """
    UPDATE tbl_user
    SET img = :value
    WHERE email = :email
    AND deleted_at IS NULL
    """, nativeQuery = true)
    void setImgInUser(@Param("value") String value, @Param("email") String email);

    @Modifying
    @Transactional
    @Query(value = """
        update tbl_user
        set deleted_at = :dateTime
        where email = :email
        and deleted_at is null
    """,nativeQuery = true)
    void deleteByEmail(@Param("email") String email, @Param("dateTime") Instant dateTime);

    @Modifying
    @Query(value = """
        UPDATE tbl_user
        SET img = null
        WHERE email = :email
        AND deleted_at IS NULL
    """, nativeQuery = true)
    void deleteImgByEmail(@Param("email") String email);

    @Modifying
    @Query(value = """
        update tbl_user
        set color_auditorium=:colorAuditorium, color_fleet_vehicles=:colorFleetVehicles, color_meeting_room=:colorMeetingRoom
        where email=:email
        and deleted_at is null
    """, nativeQuery = true)
    void setColors(@Param("email") String email, @Param("colorAuditorium") String colorAuditorium, @Param("colorFleetVehicles") String colorFleetVehicles, @Param("colorMeetingRoom") String colorMeetingRoom);

    @Modifying
    @Query(value = """
        update tbl_user
        set is_horizontal_navbar =:navbar
        where email =:email
        and deleted_at is null
    """,nativeQuery = true)
    void setNavbar(@Param("navbar") Boolean navbar, @Param("email") String email);

    @Modifying
    @Query(value = """
        update tbl_user
        set first_login = :firstLogin
        where email = :email
        and deleted_at is null
    """,nativeQuery = true)
    void setFirstLogin(@Param("firstLogin") boolean firstLogin, @Param("email") String email);

    @Modifying
    @Query(value = """
    update tbl_user
    set id_department =:idDepartment
    where email =:email
    and deleted_at is null
    """, nativeQuery = true)
    void setDepartment(@Param("idDepartment") Long id, @Param("email") String email);

    @Query(value = """
        select color_auditorium as colorAuditorium, color_fleet_vehicles as colorFleetVehicles, color_meeting_room as colorMeetingRoom
        from tbl_user tu
        where tu.email =:email
        and deleted_at is null
    """, nativeQuery = true)
    List<Object[]> getColors(@Param("email") String email);

    @Query(value = """
    SELECT tu.img
    FROM tbl_user tu
    WHERE tu.email = :email
    AND deleted_at IS NULL
    """, nativeQuery = true)
    String getImgInUser(@Param("email") String email);

    @Query(value = """
        select id
        from tbl_user tu
        where tu.email =:email
        and deleted_at is null
    """, nativeQuery = true)
    Long getIdByEmail(@Param("email") String email);

    @Query(value = """
    select is_horizontal_navbar as isHorizontalNavbar
    from tbl_user tu
    where tu.email =:email
    and deleted_at is null
    """, nativeQuery = true)
    Boolean getNavbar(@Param("email") String email);

    @Query(value = """
    select *
    from tbl_user
    where email =:email
    and deleted_at is null
    """, nativeQuery = true)
    UserEntity getUserByEmail(@Param("email") String email);

    @Query(value = """
    select tu.id_department
    from tbl_user tu
    where tu.email =:email
    and deleted_at is null
    """, nativeQuery = true)
    Long getIdDepartmentByEmail(@Param("email")String email);

    // usado antes de excluir um departamento: a FK fk_department_user impede
    // apagar enquanto houver usuario vinculado. Conta tambem os excluidos (deleted_at):
    // a linha continua no banco e segura a FK do mesmo jeito
    @Query(value = """
    select count(*)
    from tbl_user tu
    where tu.id_department =:idDepartment
    """, nativeQuery = true)
    long countByIdDepartment(@Param("idDepartment") Long idDepartment);

    @Query(value = """
    select tu.email
    from tbl_user tu
    where id = :id
    """,nativeQuery = true)
    String getEmailById(@Param("id") Long id);

    @Query(value = """
        with auditorium as (
           select ta.id, ta.reservation_type, ta.status,
                  ta.reservation_date, ta.start_time, ta.end_time, ta.user_name_requester, ta.id_user
           from tbl_auditorium ta
           where ta.id_user = :idUser
           and (CAST(:reservationDate as varchar(10)) is null or ta.reservation_date = :reservationDate)
           and ta.reservation_type in :types
        ), meeting as (
           select tmr.id, tmr.reservation_type, tmr.status,
                  tmr.reservation_date, tmr.start_time, tmr.end_time, tmr.user_name_requester, tmr.id_user
           from tbl_meeting_room tmr
           where tmr.id_user = :idUser
           and (CAST(:reservationDate as varchar(10)) is null or tmr.reservation_date = :reservationDate)
           and tmr.reservation_type in :types
        ), fleet as (
           select tfv.id, tfv.reservation_type, tfv.status,
                  tfv.reservation_date, tfv.exit_time, tfv.return_time, tfv.user_name_requester, tfv.id_user
           from tbl_fleet_vehicles tfv
           where tfv.id_user = :idUser
           and (CAST(:reservationDate as varchar(10)) is null or tfv.reservation_date = :reservationDate)
           and tfv.reservation_type in :types
        )
        select t.*, count(*) over() as totalReservations
        from (
            select * from auditorium
            union all select * from meeting
            union all select * from fleet
        ) t
        order by reservation_date, reservation_type, start_time, id
        limit 10 offset :pagination
    """, nativeQuery = true)
    List<ReservationsProjection> getReservationsByIdUser(@Param("idUser") Long idUser,
                                                         @Param("reservationDate") LocalDate date,
                                                         @Param("pagination") Long pag,
                                                         @Param("types") List<String> typesReservations);

}

