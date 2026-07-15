package com.clubflow.backend.member.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeGenerationMemberInvitationStatusRequest(
        @NotNull(message = "카카오톡 초대 여부를 입력해 주세요.")
        Boolean kakaoInvited,

        @NotNull(message = "디스코드 초대 여부를 입력해 주세요.")
        Boolean discordInvited
) {
}
