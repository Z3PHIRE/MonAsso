package com.monasso.app.service;

import com.monasso.app.model.ArchiveFilter;
import com.monasso.app.model.Meeting;
import com.monasso.app.model.Member;
import com.monasso.app.model.ScheduleStatus;
import com.monasso.app.repository.MeetingParticipantRepository;
import com.monasso.app.repository.MeetingRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.util.ValidationUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MemberRepository memberRepository;

    public MeetingService(
            MeetingRepository meetingRepository,
            MeetingParticipantRepository meetingParticipantRepository,
            MemberRepository memberRepository
    ) {
        this.meetingRepository = meetingRepository;
        this.meetingParticipantRepository = meetingParticipantRepository;
        this.memberRepository = memberRepository;
    }

    public List<Meeting> getMeetings(String searchQuery, boolean upcomingOnly) {
        return getMeetings(searchQuery, upcomingOnly, ArchiveFilter.ACTIVE);
    }

    public List<Meeting> getMeetings(String searchQuery, boolean upcomingOnly, ArchiveFilter archiveFilter) {
        return meetingRepository.findByCriteria(searchQuery, upcomingOnly, archiveFilter);
    }

    public List<Meeting> getUpcomingMeetings(int limit) {
        return meetingRepository.findUpcoming(LocalDate.now(), limit);
    }

    public Meeting addMeeting(
            String title,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            String organizer,
            Long responsibleMemberId,
            String agenda,
            String notes,
            ScheduleStatus status,
            String category,
            String linkedDocuments
    ) {
        Meeting meeting = buildMeeting(
                0L,
                title,
                date,
                startTime,
                endTime,
                location,
                organizer,
                responsibleMemberId,
                null,
                agenda,
                notes,
                status,
                category,
                linkedDocuments,
                false,
                0
        );
        return meetingRepository.create(meeting);
    }

    public Meeting updateMeeting(
            long meetingId,
            String title,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            String organizer,
            Long responsibleMemberId,
            String agenda,
            String notes,
            ScheduleStatus status,
            String category,
            String linkedDocuments
    ) {
        if (meetingId <= 0) {
            throw new IllegalArgumentException("Identifiant reunion invalide.");
        }
        Meeting existing = getMeeting(meetingId);
        long participantsCountValue = meetingParticipantRepository.countParticipants(meetingId);
        int currentParticipants = participantsCountValue > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) participantsCountValue;
        Meeting meeting = buildMeeting(
                meetingId,
                title,
                date,
                startTime,
                endTime,
                location,
                organizer,
                responsibleMemberId,
                null,
                agenda,
                notes,
                status,
                category,
                linkedDocuments,
                existing.archived(),
                currentParticipants
        );
        return meetingRepository.update(meeting);
    }

    public Meeting getMeeting(long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalStateException("Reunion introuvable."));
    }

    public void deleteMeeting(long meetingId) {
        if (!meetingRepository.deleteById(meetingId)) {
            throw new IllegalStateException("La reunion n'existe plus.");
        }
    }

    public void setArchived(long meetingId, boolean archived) {
        if (meetingId <= 0) {
            throw new IllegalArgumentException("Identifiant reunion invalide.");
        }
        if (!meetingRepository.setArchived(meetingId, archived)) {
            throw new IllegalStateException("La reunion n'existe plus.");
        }
    }

    public List<Member> getParticipants(long meetingId, String searchQuery) {
        return meetingParticipantRepository.findParticipants(meetingId, searchQuery);
    }

    public List<Member> getAvailableMembersForMeeting(long meetingId, String searchQuery) {
        Set<Long> participantIds = meetingParticipantRepository.findParticipantIds(meetingId);
        return memberRepository.findByCriteria(searchQuery, true)
                .stream()
                .filter(member -> !participantIds.contains(member.id()))
                .collect(Collectors.toList());
    }

    public void addParticipant(long meetingId, long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("Membre introuvable."));
        if (!member.active()) {
            throw new IllegalArgumentException("Le membre selectionne est inactif.");
        }
        meetingParticipantRepository.addParticipant(meetingId, memberId);
    }

    public void removeParticipant(long meetingId, long memberId) {
        if (!meetingParticipantRepository.removeParticipant(meetingId, memberId)) {
            throw new IllegalStateException("Le participant n'etait pas inscrit.");
        }
    }

    public long countAllMeetings() {
        return meetingRepository.countAll();
    }

    public long countUpcomingMeetings() {
        return meetingRepository.countUpcoming(LocalDate.now());
    }

    private Meeting buildMeeting(
            long id,
            String title,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            String location,
            String organizer,
            Long responsibleMemberId,
            String responsibleName,
            String agenda,
            String notes,
            ScheduleStatus status,
            String category,
            String linkedDocuments,
            boolean archived,
            int participantCount
    ) {
        String safeTitle = ValidationUtils.requireText(title, "Le titre");
        if (date == null) {
            throw new IllegalArgumentException("La date de reunion est obligatoire.");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("L'heure de debut est obligatoire.");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("L'heure de fin est obligatoire.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("L'heure de fin doit etre superieure a l'heure de debut.");
        }
        if (responsibleMemberId != null) {
            memberRepository.findById(responsibleMemberId)
                    .orElseThrow(() -> new IllegalArgumentException("Responsable introuvable."));
        }
        return new Meeting(
                id,
                safeTitle,
                date,
                startTime,
                endTime,
                ValidationUtils.normalizeOptional(location),
                ValidationUtils.normalizeOptional(organizer),
                responsibleMemberId,
                responsibleName,
                ValidationUtils.normalizeOptional(agenda),
                ValidationUtils.normalizeOptional(notes),
                status == null ? ScheduleStatus.PLANNED : status,
                ValidationUtils.normalizeOptional(category),
                ValidationUtils.normalizeOptional(linkedDocuments),
                archived,
                participantCount
        );
    }
}
