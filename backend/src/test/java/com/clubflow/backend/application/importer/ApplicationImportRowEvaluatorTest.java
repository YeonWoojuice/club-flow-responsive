package com.clubflow.backend.application.importer;

import com.clubflow.backend.application.importer.dto.ApplicationImportAnswerRequest;
import com.clubflow.backend.application.importer.dto.ApplicationImportRowRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationImportRowEvaluatorTest {

    private final ApplicationImportRowEvaluator evaluator = new ApplicationImportRowEvaluator();

    @Test
    void 같은_원본의_이메일은_대소문자와_공백을_정리해_모두_중복으로_판정한다() {
        List<ApplicationImportRowRequest> rows = List.of(
                row(2, "member@example.com", List.of(answer("motivation", "지원 동기", "첫 답변"))),
                row(3, " MEMBER@example.com ", List.of(answer("motivation", "지원 동기", "둘째 답변")))
        );

        List<ApplicationImportRowEvaluator.EvaluatedRow> result = evaluator.evaluate(
                rows, Map.of(), Set.of()
        );

        assertThat(result).allMatch(row -> row.status() == ApplicationImportRowStatus.DUPLICATE_IN_SOURCE);
    }

    @Test
    void 질문_키가_한_행에서_중복되면_잘못된_행으로_판정한다() {
        ApplicationImportRowRequest row = row(
                2,
                "member@example.com",
                List.of(
                        answer("motivation", "지원 동기", "답변"),
                        answer(" motivation ", "같은 질문", "다른 답변")
                )
        );

        ApplicationImportRowEvaluator.EvaluatedRow result = evaluator.evaluate(
                List.of(row), Map.of(), Set.of()
        ).getFirst();

        assertThat(result.status()).isEqualTo(ApplicationImportRowStatus.INVALID);
        assertThat(result.response().message()).contains("질문 키");
    }

    @Test
    void 응답_시간과_추가_질문이_없는_일반_시트_행도_가져올_수_있다() {
        ApplicationImportRowRequest row = new ApplicationImportRowRequest(
                2, "김민수", "member@example.com", null, "20230001", null, List.of()
        );

        ApplicationImportRowEvaluator.EvaluatedRow result = evaluator.evaluate(
                List.of(row), Map.of(), Set.of()
        ).getFirst();

        assertThat(result.status()).isEqualTo(ApplicationImportRowStatus.READY);
    }

    private ApplicationImportRowRequest row(
            int rowNumber,
            String email,
            List<ApplicationImportAnswerRequest> answers
    ) {
        return new ApplicationImportRowRequest(
                rowNumber,
                "김민수",
                email,
                "010-0000-0000",
                "20230001",
                Instant.parse("2026-03-01T01:00:00Z"),
                answers
        );
    }

    private ApplicationImportAnswerRequest answer(String key, String label, String value) {
        return new ApplicationImportAnswerRequest(key, label, value);
    }
}
