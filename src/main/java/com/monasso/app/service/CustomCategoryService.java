package com.monasso.app.service;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.CustomCategory;
import com.monasso.app.model.CustomCategoryValue;
import com.monasso.app.model.CustomFieldType;
import com.monasso.app.repository.CustomCategoryRepository;
import com.monasso.app.util.ValidationUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomCategoryService {

    private final CustomCategoryRepository customCategoryRepository;

    public CustomCategoryService(CustomCategoryRepository customCategoryRepository) {
        this.customCategoryRepository = customCategoryRepository;
    }

    public List<CustomCategory> getAllCategories() {
        return customCategoryRepository.findAll();
    }

    public List<CustomCategory> getCategoriesForScope(CategoryScope scope) {
        return customCategoryRepository.findByScope(scope == null ? CategoryScope.PERSON : scope);
    }

    public CustomCategory createCategory(
            String name,
            Long parentId,
            CustomFieldType fieldType,
            String listOptions,
            boolean active,
            int sortOrder,
            List<CategoryScope> scopes
    ) {
        String safeName = ValidationUtils.requireText(name, "Le nom de categorie");
        CustomFieldType safeFieldType = fieldType == null ? CustomFieldType.SHORT_TEXT : fieldType;
        List<CategoryScope> safeScopes = sanitizeScopes(scopes);
        String safeListOptions = sanitizeListOptions(safeFieldType, listOptions);
        int safeSortOrder = Math.max(sortOrder, 0);
        return customCategoryRepository.create(
                safeName,
                parentId,
                safeFieldType,
                safeListOptions,
                active,
                safeSortOrder,
                safeScopes
        );
    }

    public CustomCategory updateCategory(
            long categoryId,
            String name,
            Long parentId,
            CustomFieldType fieldType,
            String listOptions,
            boolean active,
            int sortOrder,
            List<CategoryScope> scopes
    ) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Identifiant categorie invalide.");
        }
        if (parentId != null && parentId == categoryId) {
            throw new IllegalArgumentException("Une categorie ne peut pas etre sa propre sous-categorie.");
        }
        String safeName = ValidationUtils.requireText(name, "Le nom de categorie");
        CustomFieldType safeFieldType = fieldType == null ? CustomFieldType.SHORT_TEXT : fieldType;
        List<CategoryScope> safeScopes = sanitizeScopes(scopes);
        String safeListOptions = sanitizeListOptions(safeFieldType, listOptions);
        int safeSortOrder = Math.max(sortOrder, 0);
        return customCategoryRepository.update(
                categoryId,
                safeName,
                parentId,
                safeFieldType,
                safeListOptions,
                active,
                safeSortOrder,
                safeScopes
        );
    }

    public List<CustomCategoryValue> getValues(CategoryScope scope, long targetId) {
        if (targetId <= 0) {
            return List.of();
        }
        return customCategoryRepository.findValues(scope == null ? CategoryScope.PERSON : scope, targetId);
    }

    public Map<Long, CustomCategoryValue> getValuesByCategoryId(CategoryScope scope, long targetId) {
        List<CustomCategoryValue> values = getValues(scope, targetId);
        Map<Long, CustomCategoryValue> byCategory = new LinkedHashMap<>();
        for (CustomCategoryValue value : values) {
            byCategory.put(value.categoryId(), value);
        }
        return byCategory;
    }

    public void saveValue(long categoryId, CategoryScope scope, long targetId, String rawValue) {
        if (categoryId <= 0) {
            throw new IllegalArgumentException("Categorie invalide.");
        }
        if (targetId <= 0) {
            throw new IllegalArgumentException("Cible invalide.");
        }
        CategoryScope safeScope = scope == null ? CategoryScope.PERSON : scope;
        CustomCategory category = customCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Categorie introuvable."));

        String normalized = ValidationUtils.normalizeOptional(rawValue);
        if (normalized == null) {
            customCategoryRepository.deleteValue(categoryId, safeScope, targetId);
            return;
        }

        switch (category.fieldType()) {
            case SHORT_TEXT, LIST -> customCategoryRepository.saveValue(
                    categoryId,
                    safeScope,
                    targetId,
                    category.fieldType().name(),
                    normalized,
                    null,
                    null,
                    null
            );
            case NUMBER -> {
                double parsedNumber;
                try {
                    parsedNumber = Double.parseDouble(normalized.replace(",", "."));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Valeur numerique invalide.");
                }
                customCategoryRepository.saveValue(
                        categoryId,
                        safeScope,
                        targetId,
                        category.fieldType().name(),
                        null,
                        parsedNumber,
                        null,
                        null
                );
            }
            case DATE -> {
                try {
                    LocalDate.parse(normalized);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Date invalide. Format attendu: YYYY-MM-DD.");
                }
                customCategoryRepository.saveValue(
                        categoryId,
                        safeScope,
                        targetId,
                        category.fieldType().name(),
                        null,
                        null,
                        normalized,
                        null
                );
            }
            case CHECKBOX -> {
                Boolean boolValue = parseBooleanValue(normalized);
                if (boolValue == null) {
                    throw new IllegalArgumentException("Valeur de case a cocher invalide (attendu: true/false).");
                }
                customCategoryRepository.saveValue(
                        categoryId,
                        safeScope,
                        targetId,
                        category.fieldType().name(),
                        null,
                        null,
                        null,
                        boolValue
                );
            }
            default -> throw new IllegalStateException("Type de champ personnalise non supporte.");
        }
    }

    private List<CategoryScope> sanitizeScopes(List<CategoryScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("Selectionnez au moins une cible d'activation.");
        }
        return scopes.stream().distinct().toList();
    }

    private String sanitizeListOptions(CustomFieldType fieldType, String listOptions) {
        if (fieldType != CustomFieldType.LIST) {
            return null;
        }
        String safe = ValidationUtils.normalizeOptional(listOptions);
        if (safe == null) {
            throw new IllegalArgumentException("Les options de liste sont obligatoires (separees par ';').");
        }
        return safe;
    }

    private Boolean parseBooleanValue(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("true") || normalized.equals("1") || normalized.equals("oui") || normalized.equals("yes")) {
            return true;
        }
        if (normalized.equals("false") || normalized.equals("0") || normalized.equals("non") || normalized.equals("no")) {
            return false;
        }
        return null;
    }
}
