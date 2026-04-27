package com.monasso.app.service;

import com.monasso.app.model.Member;
import com.monasso.app.model.TaskItem;
import com.monasso.app.model.TaskLinkType;
import com.monasso.app.model.TaskPriority;
import com.monasso.app.model.TaskStatus;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MeetingRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.repository.TaskRepository;
import com.monasso.app.util.ValidationUtils;

import java.time.LocalDate;
import java.util.List;

public class TaskService {

    private final TaskRepository taskRepository;
    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final MeetingRepository meetingRepository;

    public TaskService(
            TaskRepository taskRepository,
            MemberRepository memberRepository,
            EventRepository eventRepository,
            MeetingRepository meetingRepository
    ) {
        this.taskRepository = taskRepository;
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.meetingRepository = meetingRepository;
    }

    public List<TaskItem> getTasks(
            String searchQuery,
            Long assigneeMemberId,
            LocalDate dueDateTo,
            TaskStatus status
    ) {
        return taskRepository.findByCriteria(searchQuery, assigneeMemberId, dueDateTo, status);
    }

    public List<TaskItem> getUrgentTasks(LocalDate fromDate, LocalDate toDate, int limit) {
        LocalDate safeFrom = fromDate == null ? LocalDate.now() : fromDate;
        LocalDate safeTo = toDate == null ? safeFrom.plusDays(7) : toDate;
        if (safeTo.isBefore(safeFrom)) {
            safeTo = safeFrom;
        }
        return taskRepository.findUrgent(safeFrom, safeTo, limit);
    }

    public List<TaskItem> getOverdueTasks(int limit) {
        int safeLimit = limit <= 0 ? 5 : limit;
        LocalDate yesterday = LocalDate.now().minusDays(1);
        return taskRepository.findByCriteria("", null, yesterday, null)
                .stream()
                .filter(task -> task.status() != TaskStatus.DONE)
                .limit(safeLimit)
                .toList();
    }

    public TaskItem getTask(long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("Tache introuvable."));
    }

    public TaskItem addTask(
            String title,
            TaskLinkType linkType,
            Long linkedEventId,
            Long linkedMeetingId,
            Long assigneeMemberId,
            LocalDate dueDate,
            TaskPriority priority,
            TaskStatus status,
            String notes
    ) {
        TaskItem task = buildTask(
                0L,
                title,
                linkType,
                linkedEventId,
                linkedMeetingId,
                assigneeMemberId,
                dueDate,
                priority,
                status,
                notes,
                null
        );
        return taskRepository.create(task);
    }

    public TaskItem updateTask(
            long taskId,
            String title,
            TaskLinkType linkType,
            Long linkedEventId,
            Long linkedMeetingId,
            Long assigneeMemberId,
            LocalDate dueDate,
            TaskPriority priority,
            TaskStatus status,
            String notes
    ) {
        if (taskId <= 0) {
            throw new IllegalArgumentException("Identifiant tache invalide.");
        }
        TaskItem existing = getTask(taskId);
        TaskItem task = buildTask(
                taskId,
                title,
                linkType,
                linkedEventId,
                linkedMeetingId,
                assigneeMemberId,
                dueDate,
                priority,
                status,
                notes,
                existing.createdAt()
        );
        return taskRepository.update(task);
    }

    public void deleteTask(long taskId) {
        if (!taskRepository.deleteById(taskId)) {
            throw new IllegalStateException("La tache n'existe plus.");
        }
    }

    public long countOpenTasksForEvent(long eventId) {
        return taskRepository.countOpenByEvent(eventId);
    }

    public long countOpenTasksForMeeting(long meetingId) {
        return taskRepository.countOpenByMeeting(meetingId);
    }

    private TaskItem buildTask(
            long id,
            String title,
            TaskLinkType linkType,
            Long linkedEventId,
            Long linkedMeetingId,
            Long assigneeMemberId,
            LocalDate dueDate,
            TaskPriority priority,
            TaskStatus status,
            String notes,
            String createdAt
    ) {
        String safeTitle = ValidationUtils.requireText(title, "Le titre");

        TaskLinkType safeLinkType = linkType == null ? TaskLinkType.NONE : linkType;
        Long safeEventId = null;
        Long safeMeetingId = null;

        switch (safeLinkType) {
            case NONE -> {
                safeEventId = null;
                safeMeetingId = null;
            }
            case EVENT -> {
                if (linkedEventId == null || linkedEventId <= 0) {
                    throw new IllegalArgumentException("Selectionnez un evenement a lier.");
                }
                eventRepository.findById(linkedEventId)
                        .orElseThrow(() -> new IllegalArgumentException("Evenement lie introuvable."));
                safeEventId = linkedEventId;
            }
            case MEETING -> {
                if (linkedMeetingId == null || linkedMeetingId <= 0) {
                    throw new IllegalArgumentException("Selectionnez une reunion a lier.");
                }
                meetingRepository.findById(linkedMeetingId)
                        .orElseThrow(() -> new IllegalArgumentException("Reunion liee introuvable."));
                safeMeetingId = linkedMeetingId;
            }
        }

        Long safeAssigneeId = null;
        if (assigneeMemberId != null) {
            Member member = memberRepository.findById(assigneeMemberId)
                    .orElseThrow(() -> new IllegalArgumentException("Responsable introuvable."));
            if (!member.active()) {
                throw new IllegalArgumentException("Le responsable selectionne est inactif.");
            }
            safeAssigneeId = assigneeMemberId;
        }

        return new TaskItem(
                id,
                safeTitle,
                safeLinkType,
                safeEventId,
                safeMeetingId,
                null,
                safeAssigneeId,
                null,
                dueDate,
                priority == null ? TaskPriority.MEDIUM : priority,
                status == null ? TaskStatus.TODO : status,
                ValidationUtils.normalizeOptional(notes),
                createdAt
        );
    }
}
