package com.clubflow.backend.application.importer.dto;

import java.time.Instant;
import java.util.List;

public record ApplicationImportRowRequest(
        Integer rowNumber,
        String name,
        String email,
        String phone,
        String studentNumber,
        Instant submittedAt,
        List<ApplicationImportAnswerRequest> answers
) {
}
