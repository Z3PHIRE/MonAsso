package com.monasso.app.model;

import java.util.List;

public record CustomCategory(
        long id,
        String name,
        Long parentId,
        CustomFieldType fieldType,
        String listOptions,
        boolean active,
        boolean systemCategory,
        int sortOrder,
        List<CategoryScope> scopes
) {
    public boolean isRootCategory() {
        return parentId == null;
    }
}
