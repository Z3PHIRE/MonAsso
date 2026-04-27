package com.monasso.app.service;

import com.monasso.app.model.CategoryScope;
import com.monasso.app.model.ChecklistCategory;
import com.monasso.app.model.ChecklistItem;
import com.monasso.app.repository.ChecklistRepository;
import com.monasso.app.util.ValidationUtils;

import java.util.List;

public class ChecklistService {

    private final ChecklistRepository checklistRepository;

    public ChecklistService(ChecklistRepository checklistRepository) {
        this.checklistRepository = checklistRepository;
    }

    public List<ChecklistCategory> getCategories() {
        return checklistRepository.findCategories();
    }

    public ChecklistCategory ensureCategory(String categoryName) {
        String safeName = ValidationUtils.requireText(categoryName, "La categorie de checklist");
        return checklistRepository.ensureCategory(safeName);
    }

    public List<ChecklistItem> getItems(CategoryScope targetType, long targetId) {
        if (targetId <= 0) {
            return List.of();
        }
        CategoryScope safeTargetType = targetType == null ? CategoryScope.EVENT : targetType;
        return checklistRepository.findItems(safeTargetType, targetId);
    }

    public ChecklistItem addItem(CategoryScope targetType, long targetId, String categoryName, String itemLabel) {
        if (targetId <= 0) {
            throw new IllegalArgumentException("Aucune cible selectionnee pour la checklist.");
        }
        CategoryScope safeTargetType = sanitizeTargetType(targetType);
        ChecklistCategory category = ensureCategory(categoryName);
        String safeLabel = ValidationUtils.requireText(itemLabel, "Le libelle checklist");
        return checklistRepository.addItem(safeTargetType, targetId, category.id(), safeLabel);
    }

    public void setChecked(long checklistItemId, boolean checked) {
        if (checklistItemId <= 0) {
            throw new IllegalArgumentException("Item checklist invalide.");
        }
        checklistRepository.setChecked(checklistItemId, checked);
    }

    public void deleteItem(long checklistItemId) {
        if (checklistItemId <= 0) {
            throw new IllegalArgumentException("Item checklist invalide.");
        }
        if (!checklistRepository.deleteItem(checklistItemId)) {
            throw new IllegalStateException("L'item checklist n'existe plus.");
        }
    }

    private CategoryScope sanitizeTargetType(CategoryScope targetType) {
        CategoryScope safeTargetType = targetType == null ? CategoryScope.EVENT : targetType;
        if (safeTargetType != CategoryScope.EVENT && safeTargetType != CategoryScope.MEETING) {
            throw new IllegalArgumentException("La checklist dynamique est reservee aux evenements et reunions.");
        }
        return safeTargetType;
    }
}
