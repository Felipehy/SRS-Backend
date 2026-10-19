alter table tbl_meeting_room alter column is_coffee drop not null;
alter table tbl_meeting_room add column reason_outside_hours varchar(300);
alter table tbl_auditorium add column reason_outside_hours varchar(300);
alter table tbl_fleet_vehicles add column reason_outside_the_municipality varchar(300);