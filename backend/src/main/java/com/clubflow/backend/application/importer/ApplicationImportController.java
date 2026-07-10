package com.clubflow.backend.application.importer;

import com.clubflow.backend.application.importer.dto.ApplicationImportApplyResponse;
import com.clubflow.backend.application.importer.dto.ApplicationImportPreviewResponse;
import com.clubflow.backend.application.importer.dto.ApplicationImportRequest;
import com.clubflow.backend.google.GoogleSheetsService;
import com.clubflow.backend.member.retention.dto.ParsedWorkbookResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/clubs/{clubId}/application-import")
public class ApplicationImportController {

    private final ApplicationImportService applicationImportService;
    private final GoogleSheetsService googleSheetsService;

    public ApplicationImportController(
            ApplicationImportService applicationImportService,
            GoogleSheetsService googleSheetsService
    ) {
        this.applicationImportService = applicationImportService;
        this.googleSheetsService = googleSheetsService;
    }

    @GetMapping("/google-sheet/{spreadsheetId}/tables")
    public ParsedWorkbookResponse readGoogleSheet(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID clubId,
            @PathVariable String spreadsheetId
    ) {
        applicationImportService.requireClubAccess(oidcUser.getSubject(), clubId);
        return googleSheetsService.readTables(oidcUser.getSubject(), spreadsheetId);
    }

    @PostMapping("/preview")
    public ApplicationImportPreviewResponse preview(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID clubId,
            @Valid @RequestBody ApplicationImportRequest request
    ) {
        return applicationImportService.preview(oidcUser.getSubject(), clubId, request);
    }

    @PostMapping("/apply")
    public ApplicationImportApplyResponse apply(
            @AuthenticationPrincipal OidcUser oidcUser,
            @PathVariable UUID clubId,
            @Valid @RequestBody ApplicationImportRequest request
    ) {
        return applicationImportService.apply(oidcUser.getSubject(), clubId, request);
    }
}
