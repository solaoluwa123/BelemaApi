-- Insert a synthetic live transaction for Belema SSE / live-feed testing.
-- Clones the most recent row shape so required columns match the environment.
-- Safe marker: session_id starts with BELEMA-TEST-

USE ajiswitch_db;

SET @session_id = CONCAT('BELEMA-TEST-', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), '-', FLOOR(RAND() * 9000 + 1000));
SET @now = NOW();

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
SELECT
  @session_id,
  CONCAT('BELEMA-REF-', DATE_FORMAT(@now, '%Y%m%d%H%i%s')),
  COALESCE(channel_code, '3'),
  COALESCE(originator_account_number, '0123456789'),
  'Belema Test Sender',
  COALESCE(originator_kyc, '1'),
  COALESCE(originator_bvn, '22222222222'),
  1500.00,
  COALESCE(source_institution_code, '000004'),
  '00',
  COALESCE(beneficiary_account_number, '0987654321'),
  'Belema Test Beneficiary',
  COALESCE(beneficiary_kyc, '1'),
  COALESCE(beneficiary_bvn, '33333333333'),
  COALESCE(destination_institution_code, '000003'),
  'Belema SSE live stream test transaction',
  @now,
  CONCAT('NE-', DATE_FORMAT(@now, '%Y%m%d%H%i%s')),
  COALESCE(txn_duration, '1200'),
  @now,
  COALESCE(destination_node, '9082')
FROM tbl_creditfundtransfers
ORDER BY transaction_date_time DESC
LIMIT 1;

SELECT
  id,
  session_id,
  source_institution_code,
  destination_institution_code,
  amount,
  response_code,
  transaction_date_time,
  narration
FROM tbl_creditfundtransfers
WHERE session_id = @session_id;
