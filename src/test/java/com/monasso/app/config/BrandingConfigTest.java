package com.monasso.app.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrandingConfigTest {

    @Test
    void shouldSanitizeInvalidValues() {
        BrandingConfig input = new BrandingConfig(
                "   ",
                "invalid",
                "ABCDEF",
                "#12",
                ""
        );

        BrandingConfig sanitized = input.sanitized();

        assertEquals("MonAsso", sanitized.appName());
        assertEquals("#1F4A7D", sanitized.primaryColor());
        assertEquals("#ABCDEF", sanitized.secondaryColor());
        assertEquals("#FF8C42", sanitized.accentColor());
        assertEquals("logo.png", sanitized.logoPath());
    }
}
