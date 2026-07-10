package com.clubflow.backend.application.importer.dto;

import com.clubflow.backend.application.importer.ApplicationImportRowStatus;

import java.util.List;

public record ApplicationImportPreviewResponse(
        int totalCount,
        int readyCount,
        int invalidCount,
        int duplicateCount,
        int alreadyAppliedCount,
        List<ApplicationImportPreviewRowResponse> rows
) {
    public static ApplicationImportPreviewResponse from(
            List<ApplicationImportPreviewRowResponse> rows
    ) {
        return new ApplicationImportPreviewResponse(
                rows.size(),
                count(rows, ApplicationImportRowStatus.READY),
                count(rows, ApplicationImportRowStatus.INVALID),
                count(rows, ApplicationImportRowStatus.DUPLICATE_IN_SOURCE),
                count(rows, ApplicationImportRowStatus.ALREADY_APPLIED),
                rows
        );
    }

    private static int count(
            List<ApplicationImportPreviewRowResponse> rows,
            ApplicationImportRowStatus status
    ) {
        return (int) rows.stream().filter(row -> row.status() == status).count();
    }
}
