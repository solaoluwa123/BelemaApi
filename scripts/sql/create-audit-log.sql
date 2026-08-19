-- Durable admin audit trail for transgateweb_db
-- Apply via SSH tunnel, e.g.:
--   mysql -h 127.0.0.1 -P 13306 -u ajipay -p transgateweb_db < create-audit-log.sql

USE transgateweb_db;

CREATE TABLE IF NOT EXISTS tbl_audit_log (
  id              BIGINT       NOT NULL AUTO_INCREMENT,
  event_time      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  actor_username  VARCHAR(50)  NULL,
  actor_email     VARCHAR(100) NULL,
  actor_role      INT          NULL,
  action          VARCHAR(40)  NOT NULL,
  resource        VARCHAR(80)  NULL,
  http_method     VARCHAR(10)  NULL,
  request_path    VARCHAR(512) NULL,
  ip_address      VARCHAR(64)  NULL,
  user_agent      VARCHAR(255) NULL,
  outcome         VARCHAR(20)  NOT NULL,
  http_status     INT          NULL,
  details         TEXT         NULL,
  PRIMARY KEY (id),
  KEY idx_audit_event_time (event_time),
  KEY idx_audit_actor_email_time (actor_email, event_time),
  KEY idx_audit_action_time (action, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
