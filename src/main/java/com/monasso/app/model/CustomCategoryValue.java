package com.monasso.app.model;

public record CustomCategoryValue(
        long id,
        long categoryId,
        CategoryScope scope,
        long targetId,
        String valueType,
        String textValue,
        Double numberValue,
        String dateValue,
        Boolean boolValue
) {
}
