-- 1. Migra os dados EM_ATENDIMENTO para um status válido ANTES de trocar a constraint.
--    (troque 'APROVADA' pelo status correto conforme a regra do cliente)
UPDATE tbl_auditorium     SET status = 'APROVADA' WHERE status = 'EM_ATENDIMENTO';
UPDATE tbl_fleet_vehicles SET status = 'APROVADA' WHERE status = 'EM_ATENDIMENTO';
UPDATE tbl_meeting_room   SET status = 'APROVADA' WHERE status = 'EM_ATENDIMENTO';

-- 2. Remove as constraints antigas (que ainda aceitam EM_ATENDIMENTO).
ALTER TABLE tbl_auditorium     DROP CONSTRAINT tbl_auditorium_status_check;
ALTER TABLE tbl_fleet_vehicles DROP CONSTRAINT tbl_fleet_vehicles_status_check;
ALTER TABLE tbl_meeting_room   DROP CONSTRAINT tbl_meeting_room_status_check;

-- 3. Recria as constraints sem EM_ATENDIMENTO.
ALTER TABLE tbl_auditorium ADD CONSTRAINT tbl_auditorium_status_check
    CHECK (status IN ('ENVIADA_PARA_ANALISE','APROVADA','REPROVADA','CANCELADA','FINALIZADA'));
ALTER TABLE tbl_fleet_vehicles ADD CONSTRAINT tbl_fleet_vehicles_status_check
    CHECK (status IN ('ENVIADA_PARA_ANALISE','APROVADA','REPROVADA','CANCELADA','FINALIZADA'));
ALTER TABLE tbl_meeting_room ADD CONSTRAINT tbl_meeting_room_status_check
    CHECK (status IN ('ENVIADA_PARA_ANALISE','APROVADA','REPROVADA','CANCELADA','FINALIZADA'));