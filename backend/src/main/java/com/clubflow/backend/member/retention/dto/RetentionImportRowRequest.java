package com.clubflow.backend.member.retention.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RetentionImportRowRequest(
        @Min(value = 1, message = "원본 행 번호는 1 이상이어야 합니다.")
        int rowNumber,
        String name,
        String email,
        String studentNumber,
        Integer gradeLevel,
        @NotNull(message = "잔류 여부를 선택해 주세요.")
        Boolean retained
) {
    public RetentionImportRowRequest(
            int rowNumber,
            String name,
            String email,
            String studentNumber,
            Boolean retained
    ) {
        this(rowNumber, name, email, studentNumber, 1, retained);
    }
}
