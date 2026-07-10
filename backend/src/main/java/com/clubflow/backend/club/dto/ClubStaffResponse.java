package com.clubflow.backend.club.dto;

import com.clubflow.backend.club.ClubStaff;
import com.clubflow.backend.club.ClubStaffRole;
import com.clubflow.backend.club.ClubStaffStatus;

import java.time.Instant;
import java.util.UUID;

public record ClubStaffResponse(
        UUID id,
        UUID userId,
        String name,
        String email,
        ClubStaffRole role,
        ClubStaffStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static ClubStaffResponse from(ClubStaff staff) {
        return new ClubStaffResponse(
                staff.getId(),
                staff.getUser().getId(),
                staff.getUser().getName(),
                staff.getUser().getEmail(),
                staff.getRole(),
                staff.getStatus(),
                staff.getCreatedAt(),
                staff.getUpdatedAt()
        );
    }
}
