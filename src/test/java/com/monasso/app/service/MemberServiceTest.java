package com.monasso.app.service;

import com.monasso.app.model.Member;
import com.monasso.app.model.MemberStatusFilter;
import com.monasso.app.support.TestAppFixture;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberServiceTest {

    @Test
    void shouldCreateUpdateDeleteMember() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("member-service")) {
            Member created = fixture.memberService.addMember(
                    "Alice",
                    "Dupont",
                    "alice.dupont@example.org",
                    "0612345678",
                    "1 rue de Paris",
                    LocalDate.of(2025, 1, 10),
                    true,
                    "Note initiale"
            );

            assertTrue(created.id() > 0);

            fixture.memberService.updateMember(
                    created.id(),
                    "Alice",
                    "Durand",
                    "alice.durand@example.org",
                    "0699999999",
                    "2 avenue de Lyon",
                    LocalDate.of(2025, 2, 1),
                    true,
                    "Note mise a jour"
            );

            List<Member> members = fixture.memberService.getMembers("durand", MemberStatusFilter.ALL);
            assertEquals(1, members.size());
            assertEquals("Durand", members.get(0).lastName());

            fixture.memberService.deleteMember(created.id());
            assertEquals(0, fixture.memberService.getMembers("", MemberStatusFilter.ALL).size());
        }
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        try (TestAppFixture fixture = TestAppFixture.create("member-validation")) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> fixture.memberService.addMember(
                    "Alice",
                    "Dupont",
                    "email-invalide",
                    "0612345678",
                    "1 rue de Paris",
                    LocalDate.now(),
                    true,
                    null
            ));
            assertTrue(exception.getMessage().toLowerCase().contains("email"));
        }
    }
}
