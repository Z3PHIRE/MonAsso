package com.monasso.app.model;

public record ChecklistItem(
        long id,
        CategoryScope targetType,
        long targetId,
        Long categoryId,
        String categoryName,
        String label,
        boolean checked,
        int sortOrder
) {
}
