-- Wallet funding approve/reject audit log for transgateweb_db
-- Apply on Belema:
--   mysql -u ajipay -p'Admin12+' transgateweb_db < create-wallet-funding-approvals.sql
-- Or via SSH tunnel from your PC:
--   mysql -h 127.0.0.1 -P 13306 -u ajipay -p'Admin12+' transgateweb_db < create-wallet-funding-approvals.sql

USE transgateweb_db;

CREATE TABLE IF NOT EXISTS tbl_wallet_funding_approvals (
  id                BIGINT         NOT NULL AUTO_INCREMENT,
  operation_id      INT            NOT NULL,
  walletnumber      VARCHAR(10)    NOT NULL,
  approved_amount   DECIMAL(18,2)  NOT NULL,
  initiated_by      VARCHAR(50)    NOT NULL,
  approved_by       VARCHAR(50)    NOT NULL,
  approved_at       DATETIME       NOT NULL,
  action_type       VARCHAR(20)    NOT NULL,
  approval_status   VARCHAR(20)    NOT NULL,
  rejection_reason  VARCHAR(255)   NULL,
  PRIMARY KEY (id),
  KEY idx_funding_approvals_wallet (walletnumber),
  KEY idx_funding_approvals_status (approval_status),
  KEY idx_funding_approvals_approved_at (approved_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
