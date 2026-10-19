Alter table tbl_meeting_room add column id_user bigint;
Alter table tbl_meeting_room add constraint fk_meeting_room_user foreign key (id_user) references tbl_user(id);
Alter table tbl_meeting_room add column telephone varchar(15);
Alter table tbl_fleet_vehicles add column id_user bigint;
Alter table tbl_fleet_vehicles add constraint fk_fleet_vehicles_user foreign key (id_user) references tbl_user(id);
Alter table tbl_fleet_vehicles add column telephone varchar(15);