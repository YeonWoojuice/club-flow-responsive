package com.clubflow.backend.dues.dto;
import com.clubflow.backend.member.GenerationMemberStatus;
import java.time.LocalDate;
import java.util.UUID;
public record DuesMemberRowResponse(UUID memberDueId, UUID generationMemberId, String name, String studentNumber,
        GenerationMemberStatus memberStatus, String assessedAmount, String paidAmount,
        String refundedAmount, String status, LocalDate paidOn, boolean legacyPayment) {}
