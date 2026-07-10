package com.clubflow.backend.application.importer.dto;

public record ApplicationImportAnswerRequest(
        String questionKey,
        String questionLabel,
        String answerValue
) {
}
