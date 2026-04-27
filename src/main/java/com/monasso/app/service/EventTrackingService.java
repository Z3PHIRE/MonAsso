package com.monasso.app.service;

import com.monasso.app.model.EventAttendanceStatus;
import com.monasso.app.model.EventBudgetLine;
import com.monasso.app.model.EventBudgetLineType;
import com.monasso.app.model.EventBudgetPhase;
import com.monasso.app.model.EventBudgetSummary;
import com.monasso.app.model.EventDocument;
import com.monasso.app.model.EventHistoryEntry;
import com.monasso.app.model.EventParticipantAttendance;
import com.monasso.app.model.EventProgressSnapshot;
import com.monasso.app.model.EventTask;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.EventTrackingRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.util.ValidationUtils;

import java.time.LocalDate;
import java.util.List;

public class EventTrackingService {

    private final EventRepository eventRepository;
    private final MemberRepository memberRepository;
    private final EventTrackingRepository eventTrackingRepository;

    public EventTrackingService(
            EventRepository eventRepository,
            MemberRepository memberRepository,
            EventTrackingRepository eventTrackingRepository
    ) {
        this.eventRepository = eventRepository;
        this.memberRepository = memberRepository;
        this.eventTrackingRepository = eventTrackingRepository;
    }

    public List<EventParticipantAttendance> getParticipantAttendance(long eventId, String searchQuery) {
        ensureEventExists(eventId);
        return eventTrackingRepository.findParticipantsWithAttendance(eventId, searchQuery);
    }

    public void updateParticipantAttendance(long eventId, long memberId, EventAttendanceStatus attendanceStatus) {
        ensureEventExists(eventId);
        if (memberId <= 0) {
            throw new IllegalArgumentException("Participant invalide.");
        }
        EventAttendanceStatus safeStatus = attendanceStatus == null ? EventAttendanceStatus.REGISTERED : attendanceStatus;
        eventTrackingRepository.updateAttendanceStatus(eventId, memberId, safeStatus);
        addHistory(eventId, "ATTENDANCE", "Presence participant mise a jour: membre #" + memberId + " -> " + safeStatus.label());
    }

    public List<EventBudgetLine> getBudgetLines(long eventId) {
        ensureEventExists(eventId);
        return eventTrackingRepository.findBudgetLines(eventId);
    }

    public EventBudgetLine addBudgetLine(
            long eventId,
            EventBudgetLineType lineType,
            EventBudgetPhase budgetPhase,
            String category,
            String label,
            double amount,
            String notes
    ) {
        ensureEventExists(eventId);
        EventBudgetLineType safeType = lineType == null ? EventBudgetLineType.EXPENSE : lineType;
        EventBudgetPhase safePhase = budgetPhase == null ? EventBudgetPhase.PLANNED : budgetPhase;
        String safeLabel = ValidationUtils.requireText(label, "Le libelle budget");
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant budget doit etre superieur a 0.");
        }
        EventBudgetLine line = eventTrackingRepository.addBudgetLine(
                eventId,
                safeType,
                safePhase,
                ValidationUtils.normalizeOptional(category),
                safeLabel,
                amount,
                ValidationUtils.normalizeOptional(notes)
        );
        addHistory(eventId, "BUDGET", "Ajout ligne budget: " + safeType.label() + " " + safePhase.label() + " " + safeLabel);
        return line;
    }

    public void deleteBudgetLine(long eventId, long budgetLineId) {
        ensureEventExists(eventId);
        if (budgetLineId <= 0) {
            throw new IllegalArgumentException("Ligne budget invalide.");
        }
        if (!eventTrackingRepository.deleteBudgetLine(budgetLineId)) {
            throw new IllegalStateException("La ligne budget n'existe plus.");
        }
        addHistory(eventId, "BUDGET", "Suppression ligne budget #" + budgetLineId);
    }

    public EventBudgetSummary getBudgetSummary(long eventId) {
        ensureEventExists(eventId);
        return eventTrackingRepository.summarizeBudget(eventId);
    }

    public List<EventTask> getTasks(long eventId, boolean openOnly) {
        ensureEventExists(eventId);
        return eventTrackingRepository.findTasks(eventId, openOnly);
    }

    public EventTask addTask(
            long eventId,
            String title,
            String description,
            LocalDate dueDate,
            Long responsibleMemberId
    ) {
        ensureEventExists(eventId);
        String safeTitle = ValidationUtils.requireText(title, "Le titre de tache");
        if (responsibleMemberId != null) {
            memberRepository.findById(responsibleMemberId)
                    .orElseThrow(() -> new IllegalArgumentException("Responsable de tache introuvable."));
        }
        EventTask task = eventTrackingRepository.addTask(
                eventId,
                safeTitle,
                ValidationUtils.normalizeOptional(description),
                dueDate,
                responsibleMemberId
        );
        addHistory(eventId, "TASK", "Ajout tache: " + safeTitle);
        return task;
    }

    public EventTask setTaskCompleted(long eventId, long taskId, boolean completed) {
        ensureEventExists(eventId);
        if (taskId <= 0) {
            throw new IllegalArgumentException("Tache invalide.");
        }
        EventTask task = eventTrackingRepository.setTaskCompleted(taskId, completed);
        addHistory(eventId, "TASK", "Tache " + task.title() + " -> " + task.statusLabel());
        return task;
    }

    public void deleteTask(long eventId, long taskId) {
        ensureEventExists(eventId);
        if (taskId <= 0) {
            throw new IllegalArgumentException("Tache invalide.");
        }
        if (!eventTrackingRepository.deleteTask(taskId)) {
            throw new IllegalStateException("La tache n'existe plus.");
        }
        addHistory(eventId, "TASK", "Suppression tache #" + taskId);
    }

    public List<EventDocument> getDocuments(long eventId) {
        ensureEventExists(eventId);
        return eventTrackingRepository.findDocuments(eventId);
    }

    public EventDocument addDocument(long eventId, String documentName, String documentRef, String notes) {
        ensureEventExists(eventId);
        String safeName = ValidationUtils.requireText(documentName, "Le nom du document");
        EventDocument document = eventTrackingRepository.addDocument(
                eventId,
                safeName,
                ValidationUtils.normalizeOptional(documentRef),
                ValidationUtils.normalizeOptional(notes)
        );
        addHistory(eventId, "DOCUMENT", "Ajout document: " + safeName);
        return document;
    }

    public void deleteDocument(long eventId, long documentId) {
        ensureEventExists(eventId);
        if (documentId <= 0) {
            throw new IllegalArgumentException("Document invalide.");
        }
        if (!eventTrackingRepository.deleteDocument(documentId)) {
            throw new IllegalStateException("Le document n'existe plus.");
        }
        addHistory(eventId, "DOCUMENT", "Suppression document #" + documentId);
    }

    public List<EventHistoryEntry> getHistory(long eventId, int limit) {
        ensureEventExists(eventId);
        return eventTrackingRepository.findHistory(eventId, limit);
    }

    public EventProgressSnapshot getProgress(long eventId) {
        ensureEventExists(eventId);
        int participantsTotal = eventTrackingRepository.countParticipants(eventId);
        int participantsPresent = eventTrackingRepository.countParticipantsByStatus(eventId, EventAttendanceStatus.PRESENT);
        int participantsAbsent = eventTrackingRepository.countParticipantsByStatus(eventId, EventAttendanceStatus.ABSENT);
        int tasksTotal = eventTrackingRepository.countTasks(eventId);
        int tasksCompleted = eventTrackingRepository.countCompletedTasks(eventId);
        int checklistTotal = eventTrackingRepository.countChecklistItems(eventId);
        int checklistCompleted = eventTrackingRepository.countCompletedChecklistItems(eventId);
        return new EventProgressSnapshot(
                participantsTotal,
                participantsPresent,
                participantsAbsent,
                tasksTotal,
                tasksCompleted,
                checklistTotal,
                checklistCompleted
        );
    }

    public void addHistory(long eventId, String actionType, String details) {
        ensureEventExists(eventId);
        String safeAction = ValidationUtils.requireText(actionType, "Le type d'action");
        String safeDetails = ValidationUtils.normalizeOptional(details);
        eventTrackingRepository.addHistory(eventId, safeAction, safeDetails);
    }

    private void ensureEventExists(long eventId) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Evenement invalide.");
        }
        eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Evenement introuvable."));
    }
}
