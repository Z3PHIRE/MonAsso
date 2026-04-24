package com.monasso.app.service;

import com.monasso.app.model.Contribution;
import com.monasso.app.model.ContributionStatus;
import com.monasso.app.model.Member;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContributionServiceTest {

    @Test
    void shouldCreateContributionAndExposeHistory() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("contribution-service")) {
            Member member = fixture.memberService.addMember("Louis", "Renard", null, null, null, LocalDate.now(), true, null);

            Contribution created = fixture.contributionService.addContribution(
                    member.id(),
                    55.5,
                    LocalDate.of(2026, 1, 15),
                    "",
                    null,
                    "Virement",
                    "Cotisation annuelle"
            );

            assertTrue(created.id() > 0);
            assertEquals("2026", created.periodLabel());
            assertEquals(ContributionStatus.PAID, created.status());

            List<Contribution> history = fixture.contributionService.getMemberHistory(member.id());
            assertEquals(1, history.size());
            assertEquals("Virement", history.getFirst().paymentMethod());
        }
    }

    @Test
    void shouldRejectInvalidContributionAmount() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("contribution-validation")) {
            Member member = fixture.memberService.addMember("Lina", "Noel", null, null, null, LocalDate.now(), true, null);

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> fixture.contributionService.addContribution(
                    member.id(),
                    0,
                    LocalDate.now(),
                    "2026",
                    ContributionStatus.PAID,
                    null,
                    null
            ));
            assertNotNull(exception.getMessage());
            assertTrue(exception.getMessage().toLowerCase().contains("montant"));
        }
    }
}
