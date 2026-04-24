package com.monasso.app.service;

import com.monasso.app.model.DashboardMetrics;
import com.monasso.app.repository.ContributionRepository;
import com.monasso.app.repository.EventRepository;
import com.monasso.app.repository.MemberRepository;

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
        return new DashboardMetrics(
                memberRepository.count(),
                eventRepository.count(),
                contributionRepository.count(),
                contributionRepository.totalAmount()
        );
    }
}
