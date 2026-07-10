package com.clubflow.backend.member.retention.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionRequestValidationTest {

    @Test
    void 이월_미리보기는_이천_행을_초과할_수_없다() {
        RetentionImportRowRequest row = new RetentionImportRowRequest(
                2, "부원", "member@example.com", "20260001", true
        );
        RetentionPreviewRequest request = new RetentionPreviewRequest(
                UUID.randomUUID(), UUID.randomUUID(), Collections.nCopies(2_001, row)
        );

        assertThat(validate(request))
                .contains("한 번에 최대 2,000행까지 가져올 수 있습니다.");
    }

    @Test
    void 이월_확정은_이천_명을_초과할_수_없다() {
        RetentionApplyRequest request = new RetentionApplyRequest(
                UUID.randomUUID(), UUID.randomUUID(),
                Collections.nCopies(2_001, UUID.randomUUID())
        );

        assertThat(validate(request))
                .contains("한 번에 최대 2,000명까지 이월할 수 있습니다.");
    }

    private <T> java.util.List<String> validate(T request) {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(request).stream()
                    .map(violation -> violation.getMessage())
                    .toList();
        }
    }
}
