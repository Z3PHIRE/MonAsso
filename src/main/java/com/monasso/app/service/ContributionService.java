package com.monasso.app.service;

import com.monasso.app.model.Contribution;
import com.monasso.app.model.Member;
import com.monasso.app.repository.ContributionRepository;

import java.time.LocalDate;
import java.util.List;

public class ContributionService {

    private final ContributionRepository contributionRepository;

    public ContributionService(ContributionRepository contributionRepository) {
        this.contributionRepository = contributionRepository;
    }

    public List<Contribution> getAllContributions() {
        return contributionRepository.findAll();
    }

    public Contribution addContribution(Member member, double amount, LocalDate date, String paymentMethod, String notes) {
        if (member == null || member.id() <= 0) {
            throw new IllegalArgumentException("Un membre valide est obligatoire.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Le montant doit etre strictement positif.");
        }
        if (date == null) {
            throw new IllegalArgumentException("La date de cotisation est obligatoire.");
        }
        Contribution contribution = new Contribution(
                0L,
                member.id(),
                member.fullName(),
                amount,
                date,
                cleanOptional(paymentMethod),
                cleanOptional(notes)
        );
        return contributionRepository.create(contribution);
    }

    public long countPaidMembersForYear(int year) {
        return contributionRepository.countDistinctMembersForYear(year);
    }

    public void deleteContribution(long contributionId) {
        if (!contributionRepository.deleteById(contributionId)) {
            throw new IllegalStateException("La cotisation n'existe plus.");
        }
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
