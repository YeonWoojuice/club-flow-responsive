package com.clubflow.backend.member.dto;

import com.clubflow.backend.member.GenerationMember;
import com.clubflow.backend.member.GenerationMemberDuesStatus;
import com.clubflow.backend.member.GenerationMemberStatus;
import com.clubflow.backend.member.MemberJoinedSource;

import java.time.Instant;
import java.util.UUID;

public record GenerationMemberResponse(
        UUID id,
        UUID generationId,
        String generationName,
        UUID personId,
        String name,
        String email,
        String phone,
        String studentNumber,
        Integer gradeLevel,
        MemberJoinedSource joinedSource,
        GenerationMemberStatus status,
        GenerationMemberDuesStatus duesStatus,
        boolean kakaoInvited,
        boolean discordInvited,
        Instant duesStatusUpdatedAt,
        UUID duesStatusUpdatedByUserId,
        String duesStatusUpdatedByName,
        Instant createdAt
) {
    public static GenerationMemberResponse from(GenerationMember member) {
        return new GenerationMemberResponse(
                member.getId(),
                member.getGeneration().getId(),
                member.getGeneration().getName(),
                member.getPerson().getId(),
                member.getPerson().getName(),
                member.getPerson().getEmail(),
                member.getPerson().getPhone(),
                member.getPerson().getStudentNumber(),
                member.getGradeLevel(),
                member.getJoinedSource(),
                member.getStatus(),
                member.getDuesStatus(),
                member.isKakaoInvited(),
                member.isDiscordInvited(),
                member.getDuesStatusUpdatedAt(),
                member.getDuesStatusUpdatedBy() == null ? null : member.getDuesStatusUpdatedBy().getId(),
                member.getDuesStatusUpdatedBy() == null ? null : member.getDuesStatusUpdatedBy().getName(),
                member.getCreatedAt()
        );
    }
}
