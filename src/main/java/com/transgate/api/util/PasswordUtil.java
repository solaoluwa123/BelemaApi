package com.transgate.api.util;

import java.security.SecureRandom;

/**
 * Generates and validates temporary / assigned user passwords.
 * Storage encryption is done in SQL via TO_BASE64(AES_ENCRYPT(...)).
 */
public final class PasswordUtil {

    public static final int DEFAULT_LENGTH = 14;

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "@$!#%*?&-";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public static String generate(int length) {
        if (length < 4) {
            length = DEFAULT_LENGTH;
        }
        char[] chars = new char[length];
        chars[0] = UPPER.charAt(RANDOM.nextInt(UPPER.length()));
        chars[1] = LOWER.charAt(RANDOM.nextInt(LOWER.length()));
        chars[2] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        chars[3] = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
        for (int i = 4; i < length; i++) {
            chars[i] = ALL.charAt(RANDOM.nextInt(ALL.length()));
        }
        // Shuffle so required chars are not in fixed positions.
        for (int i = length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    public static boolean meetsPolicy(String raw) {
        if (raw == null || raw.length() < DEFAULT_LENGTH) {
            return false;
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (UPPER.indexOf(c) >= 0 || Character.isUpperCase(c)) {
                hasUpper = true;
            } else if (LOWER.indexOf(c) >= 0 || Character.isLowerCase(c)) {
                hasLower = true;
            } else if (DIGITS.indexOf(c) >= 0 || Character.isDigit(c)) {
                hasDigit = true;
            } else if (SYMBOLS.indexOf(c) >= 0) {
                hasSymbol = true;
            }
        }
        return hasUpper && hasLower && hasDigit && hasSymbol;
    }

    public static boolean isBcryptHash(String stored) {
        return stored != null
                && (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$"));
    }
}
