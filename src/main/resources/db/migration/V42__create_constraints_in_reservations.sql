create extension if not exists btree_gist;

alter table tbl_auditorium
add constraint auditorium_without_conflict_approved
exclude using gist (
    reservation_date with =,
    tsrange(reservation_date + start_time, reservation_date + end_time) with &&
)
where (status = 'APROVADA');

alter table tbl_meeting_room
add constraint meeting_room_without_conflict_approved
exclude using gist (
    id_room with =,
    reservation_date with =,
    tsrange(reservation_date + start_time, reservation_date+ end_time) with &&
)
where (status = 'APROVADA');
