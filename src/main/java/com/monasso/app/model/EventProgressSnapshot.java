package com.monasso.app.model;

public record EventProgressSnapshot(
        int participantsTotal,
        int participantsPresent,
        int participantsAbsent,
        int tasksTotal,
        int tasksCompleted,
        int checklistTotal,
        int checklistCompleted
) {
    public double completionRatio() {
        int denominator = tasksTotal + checklistTotal + participantsTotal;
        if (denominator <= 0) {
            return 0.0;
        }
        int numerator = tasksCompleted + checklistCompleted + participantsPresent;
        return (double) numerator / denominator;
    }

    public int openTasks() {
        return Math.max(tasksTotal - tasksCompleted, 0);
    }
}
