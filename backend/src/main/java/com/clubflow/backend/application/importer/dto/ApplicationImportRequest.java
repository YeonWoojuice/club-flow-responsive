package com.clubflow.backend.application.importer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ApplicationImportRequest(
        @NotNull(message = "학기를 선택해 주세요.")
        UUID generationId,

        @NotEmpty(message = "가져올 지원자 행이 없습니다.")
        @Size(max = 2_000, message = "한 번에 최대 2,000행까지 가져올 수 있습니다.")
        List<@Valid ApplicationImportRowRequest> rows
) {
}
