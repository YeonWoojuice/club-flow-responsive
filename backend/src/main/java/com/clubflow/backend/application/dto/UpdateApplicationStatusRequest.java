package com.clubflow.backend.application.dto;

import com.clubflow.backend.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateApplicationStatusRequest(
        @NotNull(message = "변경할 지원 상태를 입력해 주세요.")
        ApplicationStatus status,

        @Size(max = 500, message = "상태 변경 사유는 500자 이하여야 합니다.")
        String reason
) {
}
