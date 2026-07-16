package com.clubflow.backend.application.importer.dto;

import java.time.Instant;
import java.util.List;

public record ApplicationImportRowRequest(
        Integer rowNumber,
        String name,
        String email,
        String phone,
        String studentNumber,
        Integer gradeLevel,
        Instant submittedAt,
        List<ApplicationImportAnswerRequest> answers
) {
    public ApplicationImportRowRequest(
            Integer rowNumber,
            String name,
            String email,
            String phone,
            String studentNumber,
            Instant submittedAt,
            List<ApplicationImportAnswerRequest> answers
    ) {
        this(rowNumber, name, email, phone, studentNumber, 1, submittedAt, answers);
    }
}
