-- Add missing approval-submitter and wallet baseAmount columns.
-- Safe to re-run (skips columns that already exist).

-- ---------------------------------------------------------------------------
-- transgateweb_db
-- ---------------------------------------------------------------------------
USE transgateweb_db;

SET @db = 'transgateweb_db';

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_user_details_operations' AND COLUMN_NAME = 'created_by') = 0,
  'ALTER TABLE tbl_user_details_operations ADD COLUMN created_by VARCHAR(100) NULL AFTER note',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institution_contacts_operations' AND COLUMN_NAME = 'created_by') = 0,
  'ALTER TABLE tbl_financial_institution_contacts_operations ADD COLUMN created_by VARCHAR(100) NULL AFTER note',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'created_by') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN created_by VARCHAR(100) NULL AFTER note',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_wallets_operations' AND COLUMN_NAME = 'baseAmount') = 0,
  'ALTER TABLE tbl_wallets_operations ADD COLUMN baseAmount DECIMAL(18,2) NULL DEFAULT NULL AFTER balance',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- ajiswitch_db
-- ---------------------------------------------------------------------------
USE ajiswitch_db;

SET @db = 'ajiswitch_db';

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_wallets' AND COLUMN_NAME = 'baseAmount') = 0,
  'ALTER TABLE tbl_wallets ADD COLUMN baseAmount DECIMAL(18,2) NULL DEFAULT NULL AFTER balance',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
