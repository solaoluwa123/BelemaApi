-- Extra node + institution_ext fields for operator pending create.
-- Safe to re-run (skips columns that already exist).

USE transgateweb_db;

SET @db = 'transgateweb_db';

-- tbl_nodes_pendings
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_nodes_pendings' AND COLUMN_NAME = 'issettlementbank') = 0,
  'ALTER TABLE tbl_nodes_pendings ADD COLUMN issettlementbank INT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_nodes_pendings' AND COLUMN_NAME = 'serverIP') = 0,
  'ALTER TABLE tbl_nodes_pendings ADD COLUMN serverIP VARCHAR(20) NULL DEFAULT ''localhost''',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_nodes_pendings' AND COLUMN_NAME = 'neTimeout') = 0,
  'ALTER TABLE tbl_nodes_pendings ADD COLUMN neTimeout INT NULL DEFAULT 5',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_nodes_pendings' AND COLUMN_NAME = 'ftTimeout') = 0,
  'ALTER TABLE tbl_nodes_pendings ADD COLUMN ftTimeout INT NULL DEFAULT 10',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_nodes_pendings' AND COLUMN_NAME = 'canFundWallet') = 0,
  'ALTER TABLE tbl_nodes_pendings ADD COLUMN canFundWallet INT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- tbl_financial_institutions_pendings (institution_ext + wallet)
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'url') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN url VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'urlTSQ') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN urlTSQ VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'neEnvelope') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN neEnvelope VARCHAR(455) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'neResponseStartTag') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN neResponseStartTag VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'neResponseEndTag') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN neResponseEndTag VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'ftEnvelope') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN ftEnvelope VARCHAR(455) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'ftResponseStartTag') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN ftResponseStartTag VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'ftResponseEndTag') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN ftResponseEndTag VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'tsqEnvelope') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN tsqEnvelope VARCHAR(455) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'tsqResponseStartTag') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN tsqResponseStartTag VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'tsqResponseEndTag') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN tsqResponseEndTag VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'instWithWallet') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN instWithWallet INT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'walletname') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN walletname VARCHAR(255) NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'wallettype') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN wallettype INT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
