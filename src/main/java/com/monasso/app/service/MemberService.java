package com.monasso.app.service;

import com.monasso.app.model.Member;
import com.monasso.app.repository.MemberRepository;

import java.time.LocalDate;
import java.util.List;

public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    public Member addMember(String firstName, String lastName, String email, String phone, LocalDate joinDate) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("Le prenom est obligatoire.");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        LocalDate effectiveDate = joinDate == null ? LocalDate.now() : joinDate;
        Member member = new Member(
                0L,
                firstName.trim(),
                lastName.trim(),
                cleanOptional(email),
                cleanOptional(phone),
                effectiveDate
        );
        return memberRepository.create(member);
    }

    public void deleteMember(long memberId) {
        if (!memberRepository.deleteById(memberId)) {
            throw new IllegalStateException("Le membre n'existe plus.");
        }
    }

    private String cleanOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
