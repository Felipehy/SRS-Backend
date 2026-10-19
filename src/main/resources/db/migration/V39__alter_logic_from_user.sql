alter table tbl_user add column deleted_at timestamptz(6);
alter table tbl_user drop constraint tbl_user_email_key;
create unique index uk_user_email_ativo on tbl_user(email) where deleted_at is null;