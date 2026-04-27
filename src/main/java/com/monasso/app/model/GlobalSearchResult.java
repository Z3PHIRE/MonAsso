package com.monasso.app.model;

public record GlobalSearchResult(
        GlobalSearchType type,
        long targetId,
        String title,
        String subtitle,
        String details
) {
}
