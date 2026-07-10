package com.clubflow.backend.club.dto;

import com.clubflow.backend.club.ClubStaffRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateClubStaffInvitationRequest(
        @NotBlank(message = "초대할 이메일을 입력해 주세요.")
        @Email(message = "올바른 이메일 형식을 입력해 주세요.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        String email,
        @NotNull(message = "운영진 역할을 선택해 주세요.")
        ClubStaffRole role
) {
}
