package com.clubflow.backend.member.dto;

import com.clubflow.backend.member.GenerationMemberStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeGenerationMemberStatusRequest(
        @NotNull(message = "변경할 부원 상태를 선택해 주세요.")
        GenerationMemberStatus status,

        @Size(max = 500, message = "상태 변경 사유는 500자 이하로 입력해 주세요.")
        String reason
) {
}
