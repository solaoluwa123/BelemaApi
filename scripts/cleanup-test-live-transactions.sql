-- Remove Belema SSE batch load test rows (session_id prefix BT*).
USE ajiswitch_db;

DELETE FROM tbl_creditfundtransfers
WHERE session_id LIKE 'BT%'
  AND narration LIKE 'Belema SSE batch load test%';

SELECT ROW_COUNT() AS deleted_count;
