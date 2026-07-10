package com.clubflow.backend.club.dto;

import com.clubflow.backend.club.ClubStaffRole;
import jakarta.validation.constraints.NotNull;

public record ChangeClubStaffRoleRequest(
        @NotNull(message = "운영진 역할을 선택해 주세요.")
        ClubStaffRole role
) {
}
