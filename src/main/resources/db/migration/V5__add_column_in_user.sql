ALTER TABLE tbl_user ADD COLUMN id_room integer;
ALTER TABLE tbl_user ADD CONSTRAINT id_room FOREIGN KEY (id_room) REFERENCES tbl_room(id);