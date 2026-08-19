-- Carry admin create fields through operator pending rows.
-- Safe to re-run (skips columns that already exist).

USE transgateweb_db;

SET @db = 'transgateweb_db';

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'charge_amount') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN charge_amount DECIMAL(18,2) NULL DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'vat') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN vat DECIMAL(18,2) NULL DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'password') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN password BLOB NULL DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_nodes_pendings' AND COLUMN_NAME = 'cbn_bank_account') = 0,
  'ALTER TABLE tbl_nodes_pendings ADD COLUMN cbn_bank_account VARCHAR(100) NULL DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_nodes_pendings' AND COLUMN_NAME = 'hashkey') = 0,
  'ALTER TABLE tbl_nodes_pendings ADD COLUMN hashkey VARCHAR(255) NULL DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_nodes_pendings' AND COLUMN_NAME = 'isProcessTSQ') = 0,
  'ALTER TABLE tbl_nodes_pendings ADD COLUMN isProcessTSQ INT NULL DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
