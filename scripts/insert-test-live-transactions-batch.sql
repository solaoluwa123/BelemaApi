-- Insert N synthetic live transactions at once for Belema SSE / live-feed load testing.
-- Safe marker: session_id starts with BT{unix_ts}{001..N}
-- Usage: mysql ... -e "SET @batch_size = 100; SOURCE insert-test-live-transactions-batch.sql"
-- Or rely on default @batch_size below.

USE ajiswitch_db;

SET @batch_size = IFNULL(@batch_size, 100);
SET @now = NOW(3);
SET @batch_tag = REPLACE(UNIX_TIMESTAMP(@now), '.', '');

INSERT INTO tbl_creditfundtransfers (
  session_id,
  payment_reference,
  channel_code,
  originator_account_number,
  originator_account_name,
  originator_kyc,
  originator_bvn,
  amount,
  source_institution_code,
  response_code,
  beneficiary_account_number,
  beneficiary_account_name,
  beneficiary_kyc,
  beneficiary_bvn,
  destination_institution_code,
  narration,
  transaction_date_time,
  name_enquiry_ref,
  txn_duration,
  response_date_time,
  destination_node
)
WITH RECURSIVE nums AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM nums WHERE n < @batch_size
)
SELECT
  CONCAT('BT', @batch_tag, LPAD(nums.n, 3, '0')),
  CONCAT('BELEMA-REF-', @batch_tag, '-', LPAD(nums.n, 3, '0')),
  COALESCE(t.channel_code, '3'),
  COALESCE(t.originator_account_number, '0123456789'),
  CONCAT('Belema Load Test #', nums.n),
  COALESCE(t.originator_kyc, '1'),
  COALESCE(t.originator_bvn, '22222222222'),
  1000.00 + (nums.n MOD 500),
  COALESCE(t.source_institution_code, '000004'),
  IF(nums.n MOD 10 = 0, '09', '00'),
  COALESCE(t.beneficiary_account_number, '0987654321'),
  CONCAT('Belema Beneficiary #', nums.n),
  COALESCE(t.beneficiary_kyc, '1'),
  COALESCE(t.beneficiary_bvn, '33333333333'),
  COALESCE(t.destination_institution_code, '000003'),
  CONCAT('Belema SSE batch load test ', nums.n, '/', @batch_size),
  @now,
  CONCAT('NE-', @batch_tag, '-', LPAD(nums.n, 3, '0')),
  COALESCE(t.txn_duration, '1200'),
  @now,
  COALESCE(t.destination_node, '9082')
FROM nums
CROSS JOIN (
  SELECT
    channel_code,
    originator_account_number,
    originator_kyc,
    originator_bvn,
    source_institution_code,
    beneficiary_account_number,
    beneficiary_kyc,
    beneficiary_bvn,
    destination_institution_code,
    txn_duration,
    destination_node
  FROM tbl_creditfundtransfers
  ORDER BY transaction_date_time DESC
  LIMIT 1
) t;

SELECT
  COUNT(*) AS inserted_count,
  MIN(session_id) AS first_session_id,
  MAX(session_id) AS last_session_id,
  MIN(transaction_date_time) AS txn_time
FROM tbl_creditfundtransfers
WHERE session_id LIKE CONCAT('BT', @batch_tag, '%');
