package com.clubflow.backend.application.importer.dto;

public record ApplicationImportApplyResponse(
        int requestedCount,
        int createdCount,
        int skippedCount
) {
}
