-- Unique institution codes: two institutions cannot share the same code.
-- Safe to re-run.

-- transgateweb_db.tbl_financial_institutions.code
SET @db = 'transgateweb_db';
SET @tbl = 'tbl_financial_institutions';
SET @col = 'code';
SET @idx = 'uk_tbl_financial_institutions_code';

SET @exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = @tbl AND INDEX_NAME = @idx
);
SET @sql = IF(@exists = 0,
  'ALTER TABLE transgateweb_db.tbl_financial_institutions ADD UNIQUE KEY uk_tbl_financial_institutions_code (code)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ajiswitch_db.tbl_nodes.institution_code
SET @db = 'ajiswitch_db';
SET @tbl = 'tbl_nodes';
SET @idx = 'uk_tbl_nodes_institution_code';

SET @exists = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = @db AND TABLE_NAME = @tbl AND INDEX_NAME = @idx
);
SET @sql = IF(@exists = 0,
  'ALTER TABLE ajiswitch_db.tbl_nodes ADD UNIQUE KEY uk_tbl_nodes_institution_code (institution_code)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
