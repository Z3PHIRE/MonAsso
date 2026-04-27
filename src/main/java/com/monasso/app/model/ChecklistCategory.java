package com.monasso.app.model;

public record ChecklistCategory(
        long id,
        String name,
        boolean active,
        int sortOrder
) {
    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
