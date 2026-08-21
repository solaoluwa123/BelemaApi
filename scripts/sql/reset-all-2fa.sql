-- Reset all users so everyone must enroll 2FA on next login.
-- two_fa_secret is NOT NULL on Belema; use empty string.

UPDATE sparkpayweb_db.tbl_users
SET two_fa_enabled = 0,
    two_fa_secret = '';
