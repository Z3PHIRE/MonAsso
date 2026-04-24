package com.monasso.app.service;

import com.monasso.app.model.Member;
import com.monasso.app.model.MemberStatusFilter;
import com.monasso.app.repository.MemberRepository;
import com.monasso.app.util.ValidationUtils;

import java.time.LocalDate;
import java.util.List;

public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public List<Member> getMembers(String searchQuery, MemberStatusFilter statusFilter) {
        Boolean activeFilter = switch (statusFilter == null ? MemberStatusFilter.ALL : statusFilter) {
            case ALL -> null;
            case ACTIVE -> true;
            case INACTIVE -> false;
        };
        return memberRepository.findByCriteria(searchQuery, activeFilter);
    }

    public List<Member> getActiveMembers(String searchQuery) {
        return memberRepository.findByCriteria(searchQuery, true);
    }

    public Member addMember(
            String firstName,
            String lastName,
            String email,
            String phone,
            String address,
            LocalDate joinDate,
            boolean active,
            String notes
    ) {
        Member prepared = buildMember(0L, firstName, lastName, email, phone, address, joinDate, active, notes);
        return memberRepository.create(prepared);
    }

    public Member updateMember(
            long memberId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String address,
            LocalDate joinDate,
            boolean active,
            String notes
    ) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("Identifiant membre invalide.");
        }
        Member prepared = buildMember(memberId, firstName, lastName, email, phone, address, joinDate, active, notes);
        return memberRepository.update(prepared);
    }

    public void deleteMember(long memberId) {
        if (!memberRepository.deleteById(memberId)) {
            throw new IllegalStateException("Le membre n'existe plus.");
        }
    }

    public long countAllMembers() {
        return memberRepository.countAll();
    }

    public long countActiveMembers() {
        return memberRepository.countActive();
    }

    private Member buildMember(
            long id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String address,
            LocalDate joinDate,
            boolean active,
            String notes
    ) {
        String safeFirstName = ValidationUtils.requireText(firstName, "Le prenom");
        String safeLastName = ValidationUtils.requireText(lastName, "Le nom");
        LocalDate safeJoinDate = joinDate == null ? LocalDate.now() : joinDate;
        String safeEmail = ValidationUtils.validateOptionalEmail(email);
        String safePhone = ValidationUtils.validateOptionalPhone(phone);
        String safeAddress = ValidationUtils.normalizeOptional(address);
        String safeNotes = ValidationUtils.normalizeOptional(notes);

        return new Member(
                id,
                safeFirstName,
                safeLastName,
                safeEmail,
                safePhone,
                safeAddress,
                safeJoinDate,
                active,
                safeNotes
        );
    }
}
