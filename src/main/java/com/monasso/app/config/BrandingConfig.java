package com.monasso.app.config;

import com.monasso.app.util.ColorUtils;

public record BrandingConfig(
        String appName,
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String logoPath
) {

    private static final String DEFAULT_APP_NAME = "MonAsso";
    private static final String DEFAULT_PRIMARY = "#1F4A7D";
    private static final String DEFAULT_SECONDARY = "#F4F7FB";
    private static final String DEFAULT_ACCENT = "#FF8C42";
    private static final String DEFAULT_LOGO_PATH = "logo.png";

    public static BrandingConfig defaults() {
        return new BrandingConfig(DEFAULT_APP_NAME, DEFAULT_PRIMARY, DEFAULT_SECONDARY, DEFAULT_ACCENT, DEFAULT_LOGO_PATH);
    }

    public BrandingConfig sanitized() {
        String safeName = appName == null || appName.isBlank() ? DEFAULT_APP_NAME : appName.trim();
        String safePrimary = ColorUtils.sanitizeHexColor(primaryColor, DEFAULT_PRIMARY);
        String safeSecondary = ColorUtils.sanitizeHexColor(secondaryColor, DEFAULT_SECONDARY);
        String safeAccent = ColorUtils.sanitizeHexColor(accentColor, DEFAULT_ACCENT);
        String safeLogo = logoPath == null || logoPath.isBlank() ? DEFAULT_LOGO_PATH : logoPath.trim();
        return new BrandingConfig(safeName, safePrimary, safeSecondary, safeAccent, safeLogo);
    }
}
