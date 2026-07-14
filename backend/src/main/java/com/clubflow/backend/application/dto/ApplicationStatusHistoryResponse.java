package com.clubflow.backend.application.dto;

import com.clubflow.backend.application.ApplicationStatus;
import com.clubflow.backend.application.ApplicationStatusHistory;

import java.time.Instant;
import java.util.UUID;

public record ApplicationStatusHistoryResponse(
        UUID id,
        ApplicationStatus previousStatus,
        ApplicationStatus newStatus,
        String reason,
        UUID changedByUserId,
        String changedByName,
        Instant changedAt
) {
    public static ApplicationStatusHistoryResponse from(ApplicationStatusHistory history) {
        return new ApplicationStatusHistoryResponse(
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
