package com.siurbinfo.srs.repository.reservation;

import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.entity.reservation.FleetVehicleEntity;
import com.siurbinfo.srs.entity.reservation.MeetingRoomEntity;
import com.siurbinfo.srs.repository.reservation.projection.FinishReservationsProjection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskFinishReservation extends Repository<AuditoriumEntity, Long> {

    @Modifying
    @Query(value = """
        with get_auditorium as (
        	select ta.id,
        	ta.reservation_type,
        	ta.reservation_date,
        	ta.status\s
        	from tbl_auditorium ta
        	where (ta.reservation_date + ta.end_time) <= NOW()
        	and ta.status = 'APROVADA'
        	limit 20
        	for update skip LOCKED
        ),
        upt_auditorium as (
        	update tbl_auditorium ta
        	set status = 'FINALIZADA'
        	from get_auditorium sa
        	where ta.id = sa.id
        	returning ta.id, sa.reservation_date, sa.status, sa.reservation_type
        ),
        get_meeting as (
        	select tmr.id,
        	tmr.reservation_type,
        	tmr.reservation_date,
        	tmr.status\s
        	from tbl_meeting_room tmr\s
        	where (tmr .reservation_date + tmr .end_time) <= NOW()
        	and tmr .status = 'APROVADA'
        	limit 20
        	for update skip LOCKED
        ),
        upt_meeting as (
        	update tbl_meeting_room tmr
        	set status = 'FINALIZADA'
        	from get_meeting gm
        	where tmr.id = gm.id
        	returning tmr.id, gm.reservation_date, gm.status, gm.reservation_type
        ),
        get_fleet_vehicle as (
        	select tfv.id,
        	tfv.reservation_type,
        	tfv.reservation_date,
        	tfv.status
        	from tbl_fleet_vehicles tfv
        	where tfv.reservation_date < CURRENT_DATE
        	and tfv.status = 'APROVADA'
        	limit 20
        	for update skip locked
        ),
        upt_fleet_vehicle as (
        	update tbl_fleet_vehicles tfv\s
        	set status = 'FINALIZADA'
        	from get_fleet_vehicle gfv
        	where tfv.id = gfv.id
        	returning tfv.id, gfv.reservation_date, gfv.status, gfv.reservation_type
        )
        select * from upt_auditorium
        union all select * from upt_meeting
        union all select * from upt_fleet_vehicle
        order by reservation_date, reservation_type
    """, nativeQuery = true)
    List<FinishReservationsProjection> setFinishReservationsApproves();

    @Query(value = """
        select *
        from tbl_auditorium ta
        where id in :ids
    """, nativeQuery = true)
    List<AuditoriumEntity> getAllReservationsAuditorium(@Param("ids") List<Long> ids);

    @Query(value = """
        select *
        from tbl_meeting_room tmr
        where id in :ids
    """, nativeQuery = true)
    List<MeetingRoomEntity> getAllReservationsMeetingRoom(@Param("ids") List<Long> ids);

    @Query(value = """
        select *
        from tbl_fleet_vehicles tfv
        where id in :ids
    """, nativeQuery = true)
    List<FleetVehicleEntity> getAllReservationsFleetVehicle(@Param("ids") List<Long> ids);


}
