package com.siurbinfo.srs.repository.reservation;

import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.repository.user.ReservationsProjection;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends Repository<AuditoriumEntity, Long> {

    @Query(value = """
        select t.*, count(*) over() as totalReservations
        from (
            select ta.id, ta.reservation_type, ta.status, ta.reservation_date,
                   ta.start_time, ta.end_time, ta.user_name_requester, ta.id_user
            from tbl_auditorium ta
            union all
            select tmr.id, tmr.reservation_type, tmr.status, tmr.reservation_date,
                   tmr.start_time, tmr.end_time, tmr.user_name_requester, tmr.id_user
            from tbl_meeting_room tmr
            union all
            select tfv.id, tfv.reservation_type, tfv.status, tfv.reservation_date,
                   tfv.exit_time, tfv.return_time, tfv.user_name_requester, tfv.id_user
            from tbl_fleet_vehicles tfv
            where tfv.id_user = :idUser
        ) t
        where t.reservation_date >= cast(:startDate as date)
          and (cast(:endDate as date) is null or t.reservation_date <= cast(:endDate as date))
        order by reservation_date, reservation_type, start_time, id
    """, nativeQuery = true)
    List<ReservationsProjection> getAllReservations(@Param("idUser") Long idUser, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

}
