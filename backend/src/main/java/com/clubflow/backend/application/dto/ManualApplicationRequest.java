package com.clubflow.backend.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ManualApplicationRequest(
        @NotNull(message = "학기를 선택해 주세요.")
        UUID generationId,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다.")
        String name,

        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식을 확인해 주세요.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,

        @Size(max = 30, message = "연락처는 30자 이하여야 합니다.")
        String phone,

        @NotBlank(message = "학번을 입력해 주세요.")
        @Size(max = 50, message = "학번은 50자 이하여야 합니다.")
        String studentNumber,

        @NotNull(message = "학년을 입력해 주세요.")
        @Min(value = 1, message = "학년은 1 이상이어야 합니다.")
        @Max(value = 20, message = "학년은 20 이하여야 합니다.")
        Integer gradeLevel,

        @NotEmpty(message = "지원서 답변을 하나 이상 입력해 주세요.")
        List<@Valid ApplicationAnswerRequest> applicationAnswers
) {
    public ManualApplicationRequest(
            UUID generationId,
            String name,
            String email,
            String phone,
            String studentNumber,
            List<ApplicationAnswerRequest> applicationAnswers
    ) {
        this(generationId, name, email, phone, studentNumber, 1, applicationAnswers);
    }
}
