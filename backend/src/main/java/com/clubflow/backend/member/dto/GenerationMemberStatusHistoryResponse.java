package com.clubflow.backend.member.dto;

import com.clubflow.backend.member.GenerationMemberStatus;
import com.clubflow.backend.member.GenerationMemberStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record GenerationMemberStatusHistoryResponse(
        UUID id,
        GenerationMemberStatus previousStatus,
        GenerationMemberStatus newStatus,
        String reason,
        UUID changedByUserId,
        String changedByName,
        Instant changedAt
) {
    public static GenerationMemberStatusHistoryResponse from(GenerationMemberStatusHistory history) {
        return new GenerationMemberStatusHistoryResponse(
                history.getId(),
                history.getPreviousStatus(),
                history.getNewStatus(),
                history.getReason(),
                history.getChangedBy().getId(),
                history.getChangedBy().getName(),
                history.getChangedAt()
        );
    }
}
