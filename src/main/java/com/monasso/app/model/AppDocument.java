package com.monasso.app.model;

public record AppDocument(
        long id,
        DocumentTargetType targetType,
        long targetId,
        String targetLabel,
        String filePath,
        String fileName,
        String fileType,
        String notes,
        String addedAt
) {
}
