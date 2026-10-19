Alter table tbl_auditorium add column id_user bigint;
Alter table tbl_auditorium add constraint fk_auditorium_user foreign key (id_user) references tbl_user(id);
Alter table tbl_auditorium add column num_external_people int;
Alter table tbl_auditorium add column telephone varchar(15);