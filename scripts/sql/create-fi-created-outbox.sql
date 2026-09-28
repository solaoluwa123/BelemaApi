-- Durable outbox for financial-institution-created queue events (transgateweb_db).
-- Successful publishes delete the row; PENDING rows are retried by FiCreatedOutboxPublisher.
-- Apply via SSH tunnel, e.g.:
--   mysql -h 127.0.0.1 -P 13306 -u ajipay -p transgateweb_db < create-fi-created-outbox.sql

USE transgateweb_db;

CREATE TABLE IF NOT EXISTS tbl_financial_institution_created_outbox (
  id                 BIGINT        NOT NULL AUTO_INCREMENT,
  institution_code   VARCHAR(6)    NOT NULL,
  institution_name   VARCHAR(255)  NULL,
  email              VARCHAR(255)  NULL,
  password           TEXT          NULL,
  hash_key           VARCHAR(64)   NULL,
  event_type         VARCHAR(64)   NOT NULL,
  event_created_at   VARCHAR(40)   NULL,
  status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  attempt_count      INT           NOT NULL DEFAULT 0,
  last_error         VARCHAR(512)  NULL,
  published_at       DATETIME(6)   NULL,
  date_created       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  date_updated       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_fi_created_outbox_code (institution_code),
  KEY idx_fi_created_outbox_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
