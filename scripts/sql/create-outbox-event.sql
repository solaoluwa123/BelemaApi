-- Transactional outbox for every RabbitMQ message the API publishes (transgateweb_db).
-- Rows are written in the same transaction as the FI / user they describe. OutboxEventPublisher publishes them
-- and deletes each row once RabbitMQ confirms it. payload is the message JSON, AES-encrypted (it holds passwords).
-- Apply BEFORE deploying the API version that uses it. Apply via SSH tunnel, e.g.:
--   mysql -h 127.0.0.1 -P 13306 -u ajipay -p transgateweb_db < create-outbox-event.sql
--
-- Replaces tbl_financial_institution_created_outbox. Check that table has no PENDING rows before deploying;
-- it can be dropped once the new version is live.

USE transgateweb_db;

CREATE TABLE IF NOT EXISTS tbl_outbox_event (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  event_type      VARCHAR(64)   NOT NULL,
  event_key       VARCHAR(255)  NULL,
  exchange_name   VARCHAR(255)  NOT NULL,
  routing_key     VARCHAR(255)  NOT NULL,
  payload         MEDIUMTEXT    NOT NULL,
  status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  attempt_count   INT           NOT NULL DEFAULT 0,
  last_error      VARCHAR(512)  NULL,
  date_created    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  date_updated    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_outbox_event_status (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
