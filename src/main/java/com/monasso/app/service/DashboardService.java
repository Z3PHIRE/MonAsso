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
        String currentPeriod = String.valueOf(LocalDate.now().getYear());

        long totalMembers = memberRepository.countAll();
        long activeMembers = memberRepository.countActive();
        long totalEvents = eventRepository.countAll();
        long paidContributions = contributionRepository.countPaidMembersForPeriod(currentPeriod);
        long pendingContributions = Math.max(activeMembers - paidContributions, 0);
        long upcomingEventsCount = eventRepository.countUpcoming(LocalDate.now());
        double totalContributionAmount = contributionRepository.totalAmountForPeriod(currentPeriod);

        return new DashboardMetrics(
                totalMembers,
                activeMembers,
                totalEvents,
                paidContributions,
                pendingContributions,
                upcomingEventsCount,
                totalContributionAmount,
                currentPeriod,
                eventRepository.findUpcoming(LocalDate.now(), 5)
        );
    }
}
