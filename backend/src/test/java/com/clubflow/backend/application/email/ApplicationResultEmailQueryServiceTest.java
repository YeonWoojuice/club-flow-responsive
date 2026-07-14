package com.clubflow.backend.application.email;

import com.clubflow.backend.application.ApplicationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApplicationResultEmailQueryServiceTest {

    @Test
    void 현재_지원_결과와_같은_메일의_최신_상태만_사용한다() {
        ApplicationResultEmailMessageRepository repository = mock(ApplicationResultEmailMessageRepository.class);
        ApplicationResultEmailQueryService service = new ApplicationResultEmailQueryService(repository);
        UUID applicationId = UUID.randomUUID();
        ApplicationResultEmailMessage oldAccepted = message(
                applicationId, ApplicationStatus.ACCEPTED, ApplicationResultEmailMessageStatus.SENT,
                Instant.parse("2026-07-14T01:00:00Z")
        );
        ApplicationResultEmailMessage currentRejected = message(
                applicationId, ApplicationStatus.REJECTED, ApplicationResultEmailMessageStatus.FAILED, null
        );
        when(repository.findAllByApplication_IdInOrderByCreatedAtDesc(java.util.Set.of(applicationId)))
                .thenReturn(List.of(currentRejected, oldAccepted));

        ApplicationResultEmailQueryService.ResultEmailState state = service.latestStates(
                Map.of(applicationId, ApplicationStatus.REJECTED)
        ).get(applicationId);

        assertThat(state.status()).isEqualTo(ApplicationResultEmailStatus.FAILED);
        assertThat(state.sentAt()).isNull();
    }

    private ApplicationResultEmailMessage message(
            UUID applicationId,
            ApplicationStatus decision,
            ApplicationResultEmailMessageStatus status,
            Instant sentAt
    ) {
        ApplicationResultEmailMessage message = mock(ApplicationResultEmailMessage.class);
        when(message.getApplicationId()).thenReturn(applicationId);
        when(message.getDecision()).thenReturn(decision);
        when(message.getStatus()).thenReturn(status);
        when(message.getSentAt()).thenReturn(sentAt);
        return message;
    }
}
