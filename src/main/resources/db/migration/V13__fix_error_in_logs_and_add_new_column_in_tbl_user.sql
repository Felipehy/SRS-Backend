ALTER TABLE tbl_logs DROP COLUMN is_horizontal_navbar;
ALTER TABLE tbl_user ADD COLUMN is_horizontal_navbar boolean DEFAULT TRUE;