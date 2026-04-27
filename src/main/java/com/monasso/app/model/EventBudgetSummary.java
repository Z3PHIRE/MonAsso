package com.monasso.app.model;

public record EventBudgetSummary(
        double plannedRevenue,
        double plannedExpense,
        double actualRevenue,
        double actualExpense
) {
    public double totalPlanned() {
        return plannedRevenue - plannedExpense;
    }

    public double totalActual() {
        return actualRevenue - actualExpense;
    }

    public double remainingOrOverrun() {
        return totalPlanned() - totalActual();
    }
}
