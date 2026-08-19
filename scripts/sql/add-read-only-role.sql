-- Read Only platform role (id 9) for transgateweb_db
-- Run on production BEFORE deploying API:
--   SELECT id, role_name FROM tbl_role ORDER BY id;
-- Confirm id 9 is unused, then:
--   mysql -u ... transgateweb_db < add-read-only-role.sql

USE transgateweb_db;

-- 1) Role row
INSERT INTO tbl_role (id, role_name, date_created)
SELECT 9, 'Read Only', NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM tbl_role WHERE id = 9);

-- 2) Clone Operator (role_id = 2) menu tree to Read Only (role_id = 9)
-- Parent menus
INSERT INTO tbl_menus (role_id, label, icon, path, parent_id, access)
SELECT 9, m.label, m.icon, m.path, NULL, m.access
FROM tbl_menus m
WHERE m.role_id = 2
  AND m.parent_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM tbl_menus x
    WHERE x.role_id = 9 AND x.parent_id IS NULL AND x.label = m.label AND x.path = m.path
  );

-- Child menus (map new parent ids by label/path)
INSERT INTO tbl_menus (role_id, label, icon, path, parent_id, access)
SELECT 9, c.label, c.icon, c.path, p9.id, c.access
FROM tbl_menus c
INNER JOIN tbl_menus p2 ON c.parent_id = p2.id AND p2.role_id = 2
INNER JOIN tbl_menus p9
  ON p9.role_id = 9
  AND p9.parent_id IS NULL
  AND p9.label = p2.label
  AND IFNULL(p9.path, '') = IFNULL(p2.path, '')
WHERE c.role_id = 2
  AND c.parent_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM tbl_menus x
    WHERE x.role_id = 9
      AND x.parent_id = p9.id
      AND x.label = c.label
      AND IFNULL(x.path, '') = IFNULL(c.path, '')
  );

-- 3) Optional: assign Read Only to a test user (uncomment and set email)
-- UPDATE tbl_user_details SET role = 9 WHERE email_address = 'readonly.test@example.com' AND deleted = 0;
