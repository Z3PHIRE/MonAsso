package com.monasso.app.service;

import com.monasso.app.model.Member;
import com.monasso.app.model.MemberStatusFilter;
import com.monasso.app.model.PersonType;
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
        Member prepared = buildMember(
                0L,
                firstName,
                lastName,
                PersonType.MEMBER,
                phone,
                email,
                active,
                address,
                joinDate,
                null,
                null,
                null,
                notes,
                null,
                null,
                null,
                null,
                null
        );
        return memberRepository.create(prepared);
    }

    public Member addMember(
            String firstName,
            String lastName,
            PersonType personType,
            String phone,
            String email,
            boolean active,
            String address,
            LocalDate joinDate,
            String associationRole,
            String skills,
            String availability,
            String notes,
            String emergencyContact,
            String clothingSize,
            String certifications,
            String constraintsInfo,
            String linkedDocuments
    ) {
        Member prepared = buildMember(
                0L,
                firstName,
                lastName,
                personType,
                phone,
                email,
                active,
                address,
                joinDate,
                associationRole,
                skills,
                availability,
                notes,
                emergencyContact,
                clothingSize,
                certifications,
                constraintsInfo,
                linkedDocuments
        );
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
        Member prepared = buildMember(
                memberId,
                firstName,
                lastName,
                PersonType.MEMBER,
                phone,
                email,
                active,
                address,
                joinDate,
                null,
                null,
                null,
                notes,
                null,
                null,
                null,
                null,
                null
        );
        return memberRepository.update(prepared);
    }

    public Member updateMember(
            long memberId,
            String firstName,
            String lastName,
            PersonType personType,
            String phone,
            String email,
            boolean active,
            String address,
            LocalDate joinDate,
            String associationRole,
            String skills,
            String availability,
            String notes,
            String emergencyContact,
            String clothingSize,
            String certifications,
            String constraintsInfo,
            String linkedDocuments
    ) {
        if (memberId <= 0) {
            throw new IllegalArgumentException("Identifiant membre invalide.");
        }
        Member prepared = buildMember(
                memberId,
                firstName,
                lastName,
                personType,
                phone,
                email,
                active,
                address,
                joinDate,
                associationRole,
                skills,
                availability,
                notes,
                emergencyContact,
                clothingSize,
                certifications,
                constraintsInfo,
                linkedDocuments
        );
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
            PersonType personType,
            String phone,
            String email,
            boolean active,
            String address,
            LocalDate joinDate,
            String associationRole,
            String skills,
            String availability,
            String notes,
            String emergencyContact,
            String clothingSize,
            String certifications,
            String constraintsInfo,
            String linkedDocuments
    ) {
        String safeFirstName = ValidationUtils.requireText(firstName, "Le prenom");
        String safeLastName = ValidationUtils.requireText(lastName, "Le nom");
        LocalDate safeJoinDate = joinDate == null ? LocalDate.now() : joinDate;
        PersonType safeType = personType == null ? PersonType.MEMBER : personType;
        String safeEmail = ValidationUtils.validateOptionalEmail(email);
        String safePhone = ValidationUtils.validateOptionalPhone(phone);
        String safeAddress = ValidationUtils.normalizeOptional(address);
        String safeRole = ValidationUtils.normalizeOptional(associationRole);
        String safeSkills = ValidationUtils.normalizeOptional(skills);
        String safeAvailability = ValidationUtils.normalizeOptional(availability);
        String safeNotes = ValidationUtils.normalizeOptional(notes);
        String safeEmergencyContact = ValidationUtils.normalizeOptional(emergencyContact);
        String safeClothingSize = ValidationUtils.normalizeOptional(clothingSize);
        String safeCertifications = ValidationUtils.normalizeOptional(certifications);
        String safeConstraintsInfo = ValidationUtils.normalizeOptional(constraintsInfo);
        String safeLinkedDocuments = ValidationUtils.normalizeOptional(linkedDocuments);

        return new Member(
                id,
                safeFirstName,
                safeLastName,
                safeType,
                safeEmail,
                safePhone,
                active,
                safeAddress,
                safeJoinDate,
                safeRole,
                safeSkills,
                safeAvailability,
                safeNotes,
                safeEmergencyContact,
                safeClothingSize,
                safeCertifications,
                safeConstraintsInfo,
                safeLinkedDocuments
        );
    }
}
