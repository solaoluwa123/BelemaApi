-- Remap legacy CBN bank codes on wallets to registered system institution codes.
-- Safe to re-run: only rows still carrying the old CBN code are updated.
--
-- Mapping reference (CBN code -> system code), only where the institution exists:
--   033  United Bank for Africa  -> 000004  (ajiswitch_db.tbl_nodes)
--   101  Providus Bank          -> 000023  (tbl_nodes + tbl_financial_institutions)
--   214  FCMB                   -> 000003  (tbl_nodes + tbl_financial_institutions)
--   301  Jaiz Bank              -> JAIZ    (tbl_nodes + tbl_financial_institutions)
--
-- Not remapped automatically (institution not registered in this environment):
--   044  Access Bank
--   058  Guaranty Trust Bank (GTBank)
--   394  Unknown / custom test code

USE ajiswitch_db;

-- Preview wallets that will change
SELECT 'before_wallets' AS stage, walletnumber, walletname, financialinstitutioncode
FROM tbl_wallets
WHERE financialinstitutioncode IN ('033', '101', '214', '301')
ORDER BY financialinstitutioncode, walletnumber;

USE transgateweb_db;

SELECT 'before_ops' AS stage, id, walletnumber, financialInstitutionCode, actionType
FROM tbl_wallets_operations
WHERE financialInstitutionCode IN ('033', '101', '214', '301')
ORDER BY financialInstitutionCode, id;

START TRANSACTION;

USE ajiswitch_db;

UPDATE tbl_wallets SET financialinstitutioncode = '000004' WHERE financialinstitutioncode = '033';
UPDATE tbl_wallets SET financialinstitutioncode = '000023' WHERE financialinstitutioncode = '101';
UPDATE tbl_wallets SET financialinstitutioncode = '000003' WHERE financialinstitutioncode = '214';
UPDATE tbl_wallets SET financialinstitutioncode = 'JAIZ'   WHERE financialinstitutioncode = '301';

USE transgateweb_db;

UPDATE tbl_wallets_operations SET financialInstitutionCode = '000004' WHERE financialInstitutionCode = '033';
UPDATE tbl_wallets_operations SET financialInstitutionCode = '000023' WHERE financialInstitutionCode = '101';
UPDATE tbl_wallets_operations SET financialInstitutionCode = '000003' WHERE financialInstitutionCode = '214';
UPDATE tbl_wallets_operations SET financialInstitutionCode = 'JAIZ'   WHERE financialInstitutionCode = '301';

COMMIT;

USE ajiswitch_db;

SELECT 'after_wallets' AS stage, w.walletnumber, w.walletname, w.financialinstitutioncode,
  COALESCE(NULLIF(fi.name, ''), n.institution_name, '') AS institution_name
FROM tbl_wallets w
LEFT JOIN transgateweb_db.tbl_financial_institutions fi ON w.financialinstitutioncode = fi.code
LEFT JOIN ajiswitch_db.tbl_nodes n ON w.financialinstitutioncode = n.institution_code
WHERE w.walletnumber IN ('1794888962', '8132954572', '3132954571', '303')
   OR w.financialinstitutioncode IN ('000004', '000023', '000003', 'JAIZ', '044', '058', '394')
ORDER BY w.financialinstitutioncode, w.walletnumber;

USE transgateweb_db;

SELECT 'after_ops' AS stage, id, walletnumber, financialInstitutionCode, actionType
FROM tbl_wallets_operations
WHERE financialInstitutionCode IN ('000004', '000023', '000003', 'JAIZ', '033', '101', '214', '301', '044', '058')
ORDER BY financialInstitutionCode, id;

SELECT 'still_unmapped_wallets' AS stage, walletnumber, walletname, financialinstitutioncode
FROM ajiswitch_db.tbl_wallets
WHERE financialinstitutioncode IN ('044', '058', '394')
ORDER BY financialinstitutioncode, walletnumber;
