package com.clubflow.backend.club.dto;

import com.clubflow.backend.club.ClubStaffInvitation;
import com.clubflow.backend.club.ClubStaffInvitationStatus;
import com.clubflow.backend.club.ClubStaffRole;

import java.time.Instant;
import java.util.UUID;

public record ClubStaffInvitationResponse(
        UUID id,
        UUID clubId,
        String clubName,
        String email,
        ClubStaffRole role,
        ClubStaffInvitationStatus status,
        UUID invitedByUserId,
        String invitedByName,
        Instant createdAt,
        Instant respondedAt
) {
    public static ClubStaffInvitationResponse from(ClubStaffInvitation invitation) {
        return new ClubStaffInvitationResponse(
                invitation.getId(),
                invitation.getClub().getId(),
                invitation.getClub().getName(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getInvitedBy().getId(),
                invitation.getInvitedBy().getName(),
                invitation.getCreatedAt(),
                invitation.getRespondedAt()
        );
    }
}
