alter table tbl_groups add column color_group varchar(7) default '#ffffff';
alter table tbl_groups add column description varchar(150);
insert into tbl_groups (group_cognito, group_name, description) values ('siurb-administrador','Administrador','Tudo — todos os calendários, Administração, Gestão de Frotas e Painel Admin.');
insert into tbl_groups (group_cognito, group_name, description) values ('siurb-divisao-administrativa','DA','Reservas de Salas/Auditório/Frota + aprovar/reprovar');
insert into tbl_groups (group_cognito, group_name, description) values ('siurb-divisao-frotas','Frotas','Reservas de Frota + tela Gestão de Frotas');
insert into tbl_groups (group_cognito, group_name, description) values ('siurb-usuario-geral','Usuário Geral','Cria/vê reservas de Salas, Auditório e Frota');
