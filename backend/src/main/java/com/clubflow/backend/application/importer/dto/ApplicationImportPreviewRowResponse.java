package com.clubflow.backend.application.importer.dto;

import com.clubflow.backend.application.importer.ApplicationImportRowStatus;

import java.time.Instant;
import java.util.UUID;

public record ApplicationImportPreviewRowResponse(
        Integer rowNumber,
        String name,
        String email,
        String phone,
        String studentNumber,
        Integer gradeLevel,
        Instant submittedAt,
        UUID personId,
        ApplicationImportRowStatus status,
        String message
) {
}
