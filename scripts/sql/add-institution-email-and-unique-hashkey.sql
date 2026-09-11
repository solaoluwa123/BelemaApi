-- Institution email + unique hashkey
-- Safe to re-run (skips columns/indexes that already exist).

USE transgateweb_db;

SET @db = 'transgateweb_db';
SET @aj = 'ajiswitch_db';

-- Live financial institutions: email
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions' AND COLUMN_NAME = 'email') = 0,
  'ALTER TABLE tbl_financial_institutions ADD COLUMN email VARCHAR(255) NULL DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Pending financial institutions: email
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND COLUMN_NAME = 'email') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD COLUMN email VARCHAR(255) NULL DEFAULT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Normalize empty emails/hashkeys to NULL so UNIQUE allows multiple unset rows
UPDATE tbl_financial_institutions SET email = NULL WHERE email IS NOT NULL AND TRIM(email) = '';
UPDATE tbl_financial_institutions_pendings SET email = NULL WHERE email IS NOT NULL AND TRIM(email) = '';
UPDATE ajiswitch_db.tbl_nodes SET hashkey = NULL WHERE hashkey IS NOT NULL AND TRIM(hashkey) = '';
UPDATE tbl_nodes_pendings SET hashkey = NULL WHERE hashkey IS NOT NULL AND TRIM(hashkey) = '';

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions' AND INDEX_NAME = 'uk_fi_email') = 0,
  'ALTER TABLE tbl_financial_institutions ADD UNIQUE INDEX uk_fi_email (email)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_financial_institutions_pendings' AND INDEX_NAME = 'uk_fi_pendings_email') = 0,
  'ALTER TABLE tbl_financial_institutions_pendings ADD UNIQUE INDEX uk_fi_pendings_email (email)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @aj AND TABLE_NAME = 'tbl_nodes' AND INDEX_NAME = 'uk_nodes_hashkey') = 0,
  'ALTER TABLE ajiswitch_db.tbl_nodes ADD UNIQUE INDEX uk_nodes_hashkey (hashkey)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'tbl_nodes_pendings' AND INDEX_NAME = 'uk_nodes_pendings_hashkey') = 0,
  'ALTER TABLE tbl_nodes_pendings ADD UNIQUE INDEX uk_nodes_pendings_hashkey (hashkey)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
