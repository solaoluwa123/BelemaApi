-- Forgot-password tokens (transgateweb_db). One active reset per user; a new request replaces the old one.
-- Only a SHA-256 hash of the token is stored. Rows are deleted when the reset is used or found expired.
-- The user's password in sparkpayweb_db.tbl_users is not changed until the reset is completed.
-- Apply BEFORE deploying the API version that uses it. Apply via SSH tunnel, e.g.:
--   mysql -h 127.0.0.1 -P 13306 -u ajipay -p transgateweb_db < create-password-reset.sql

USE transgateweb_db;

CREATE TABLE IF NOT EXISTS tbl_password_reset (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  username      VARCHAR(145)  NOT NULL,
  reference     VARCHAR(32)   NOT NULL,
  token_hash    CHAR(64)      NOT NULL,
  expires_at    DATETIME(6)   NOT NULL,
  date_created  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_password_reset_username (username),
  UNIQUE KEY uk_password_reset_reference (reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
