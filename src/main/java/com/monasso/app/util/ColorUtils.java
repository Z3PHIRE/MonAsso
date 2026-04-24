package com.monasso.app.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ColorUtils {

    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private ColorUtils() {
    }

    public static String sanitizeHexColor(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        if (normalized.matches("^[0-9A-Fa-f]{6}$")) {
            normalized = "#" + normalized;
        }
        if (!HEX_COLOR.matcher(normalized).matches()) {
            return fallback;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
