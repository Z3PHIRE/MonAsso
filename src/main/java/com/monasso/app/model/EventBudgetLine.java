package com.monasso.app.model;

public record EventBudgetLine(
        long id,
        long eventId,
        EventBudgetLineType lineType,
        EventBudgetPhase budgetPhase,
        String category,
        String label,
        double amount,
        String notes
) {
}
