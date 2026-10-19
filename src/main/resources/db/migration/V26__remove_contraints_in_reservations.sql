ALTER TABLE tbl_auditorium
ALTER COLUMN is_approve_coffee_break DROP NOT NULL,
ALTER COLUMN is_approve_coffee_break DROP DEFAULT;
ALTER TABLE tbl_meeting_room
ALTER COLUMN is_approve_coffee DROP NOT NULL,
ALTER COLUMN is_approve_coffee DROP DEFAULT;