alter table tbl_user drop column id_room;
alter table tbl_user add column id_department bigint;
alter table tbl_user add constraint fk_department_user foreign key (id_department) references tbl_department(id);