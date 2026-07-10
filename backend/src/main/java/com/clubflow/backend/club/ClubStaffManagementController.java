package com.clubflow.backend.club;

import com.clubflow.backend.club.dto.ChangeClubStaffRoleRequest;
import com.clubflow.backend.club.dto.ClubStaffInvitationResponse;
import com.clubflow.backend.club.dto.ClubStaffResponse;
import com.clubflow.backend.club.dto.CreateClubStaffInvitationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clubs/{clubId}")
public class ClubStaffManagementController {

    private final ClubStaffManagementService service;

    public ClubStaffManagementController(ClubStaffManagementService service) {
        this.service = service;
    }

    @GetMapping("/staff")
    public List<ClubStaffResponse> listStaff(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID clubId
    ) {
        return service.listStaff(oidcUser.getSubject(), clubId);
    }

    @PatchMapping("/staff/{staffId}/role")
    public ClubStaffResponse changeRole(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID clubId,
            @PathVariable UUID staffId,
            @Valid @RequestBody ChangeClubStaffRoleRequest request
    ) {
        return service.changeRole(oidcUser.getSubject(), clubId, staffId, request);
    }

    @DeleteMapping("/staff/{staffId}")
    public ClubStaffResponse revoke(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID clubId,
            @PathVariable UUID staffId
    ) {
        return service.revoke(oidcUser.getSubject(), clubId, staffId);
    }

    @GetMapping("/staff-invitations")
    public List<ClubStaffInvitationResponse> listInvitations(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID clubId
    ) {
        return service.listClubInvitations(oidcUser.getSubject(), clubId);
    }

    @PostMapping("/staff-invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ClubStaffInvitationResponse invite(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID clubId,
            @Valid @RequestBody CreateClubStaffInvitationRequest request
    ) {
        return service.invite(oidcUser.getSubject(), clubId, request);
    }

    @DeleteMapping("/staff-invitations/{invitationId}")
    public ClubStaffInvitationResponse cancelInvitation(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID clubId,
            @PathVariable UUID invitationId
    ) {
        return service.cancelInvitation(oidcUser.getSubject(), clubId, invitationId);
    }
}
