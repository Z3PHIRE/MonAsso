package com.monasso.app.service;

import com.monasso.app.model.Contribution;
import com.monasso.app.model.ContributionStatus;
import com.monasso.app.model.Member;
import com.monasso.app.repository.ContributionRepository;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.util.ValidationUtils;

import java.time.LocalDate;
import java.util.List;

public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final MemberRepository memberRepository;

    public ContributionService(ContributionRepository contributionRepository, MemberRepository memberRepository) {
        this.contributionRepository = contributionRepository;
        this.memberRepository = memberRepository;
    }

    public List<Contribution> getContributions(String searchQuery, String periodLabel, ContributionStatus status) {
        String effectivePeriod = periodLabel == null || periodLabel.isBlank() ? null : periodLabel.trim();
        return contributionRepository.findByCriteria(searchQuery, effectivePeriod, status);
    }

    public List<Contribution> getMemberHistory(long memberId) {
        return contributionRepository.findByMemberId(memberId);
    }

    public Contribution addContribution(
            long memberId,
            double amount,
            LocalDate contributionDate,
            String periodLabel,
            ContributionStatus status,
            String paymentMethod,
            String notes
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable."));
        if (!member.active()) {
            throw new IllegalArgumentException("Le membre selectionne est inactif.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit etre strictement positif.");
        }
        if (contributionDate == null) {
            throw new IllegalArgumentException("La date de cotisation est obligatoire.");
        }

        String safePeriod = periodLabel == null || periodLabel.isBlank()
                ? String.valueOf(contributionDate.getYear())
                : periodLabel.trim();

        Contribution safeContribution = new Contribution(
                0L,
                member.id(),
                member.fullName(),
                amount,
                contributionDate,
                safePeriod,
                status == null ? ContributionStatus.PAID : status,
                ValidationUtils.normalizeOptional(paymentMethod),
                ValidationUtils.normalizeOptional(notes)
        );
        return contributionRepository.create(safeContribution);
    }

    public void deleteContribution(long contributionId) {
        if (!contributionRepository.deleteById(contributionId)) {
            throw new IllegalStateException("La cotisation n'existe plus.");
        }
    }

    public long countPaidMembersForPeriod(String periodLabel) {
        return contributionRepository.countPaidMembersForPeriod(periodLabel);
    }

    public double totalAmountForPeriod(String periodLabel) {
        return contributionRepository.totalAmountForPeriod(periodLabel);
    }

    public String currentPeriod() {
        return String.valueOf(LocalDate.now().getYear());
    }
}
