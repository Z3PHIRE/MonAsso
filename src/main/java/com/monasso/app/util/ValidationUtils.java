package com.monasso.app.util;

import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s.]{7,20}$");

    private ValidationUtils() {
    }

    public static String requireText(String value, String fieldLabel) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + " est obligatoire.");
        }
        return value.trim();
    }

    public static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String validateOptionalEmail(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Email invalide.");
        }
        return normalized;
    }

    public static String validateOptionalPhone(String value) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            return null;
        }
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Telephone invalide.");
        }
        return normalized;
    }
}
