-- Force password change on first login for newly created users.
-- Run once against Belema / local MySQL (sparkpayweb_db).

ALTER TABLE sparkpayweb_db.tbl_users
  ADD COLUMN must_change_password TINYINT(1) NOT NULL DEFAULT 0
  AFTER enabled;
