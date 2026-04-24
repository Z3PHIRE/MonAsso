package com.monasso.app.service;

import com.monasso.app.model.DashboardMetrics;
import com.monasso.app.repository.ContributionRepository;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MemberRepository;

import java.time.LocalDate;

public class DashboardService {

    private final MemberRepository memberRepository;
    private final EventRepository eventRepository;
    private final ContributionRepository contributionRepository;

    public DashboardService(MemberRepository memberRepository, EventRepository eventRepository, ContributionRepository contributionRepository) {
        this.memberRepository = memberRepository;
        this.eventRepository = eventRepository;
        this.contributionRepository = contributionRepository;
    }

    public DashboardMetrics getMetrics() {
        long totalMembers = memberRepository.count();
        long totalEvents = eventRepository.count();
        long paidContributions = contributionRepository.countDistinctMembersForYear(LocalDate.now().getYear());
        long pendingContributions = Math.max(totalMembers - paidContributions, 0);
        long upcomingEventsCount = eventRepository.countUpcoming(LocalDate.now());

        return new DashboardMetrics(
                totalMembers,
                totalEvents,
                paidContributions,
                pendingContributions,
                upcomingEventsCount,
                contributionRepository.totalAmount(),
                eventRepository.findUpcoming(LocalDate.now(), 5)
        );
    }
}
