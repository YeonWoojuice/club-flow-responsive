package com.clubflow.backend.application.email;

import com.clubflow.backend.application.Application;
import com.clubflow.backend.application.ApplicationRepository;
import com.clubflow.backend.application.ApplicationStatus;
import com.clubflow.backend.application.email.dto.ApplicationResultEmailRequest;
import com.clubflow.backend.club.Club;
import com.clubflow.backend.club.ClubAccessService;
import com.clubflow.backend.club.ClubStaff;
import com.clubflow.backend.common.ConflictException;
import com.clubflow.backend.generation.Generation;
import com.clubflow.backend.generation.GenerationService;
import com.clubflow.backend.person.Person;
import com.clubflow.backend.member.GenerationMemberService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationResultEmailPersistenceServiceTest {

    private record Fixture(
            ApplicationResultEmailPersistenceService service,
            ApplicationRepository applicationRepository,
            ApplicationResultEmailBatchRepository batchRepository,
            ApplicationResultEmailMessageRepository messageRepository,
            ApplicationResultEmailQueryService queryService,
            ClubAccessService clubAccessService,
            GenerationService generationService,
            GenerationMemberService generationMemberService
    ) {
    }

    @Test
    void 선택_변수_값이_없는_지원자는_실제_발송_기록에서_제외한다() {
        Fixture fixture = fixture();
        UUID clubId = UUID.randomUUID();
        UUID generationId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        ClubStaff staff = mock(ClubStaff.class);
        Club club = mock(Club.class);
        Generation generation = mock(Generation.class);
        Person person = mock(Person.class);
        Application application = mock(Application.class);
        when(fixture.clubAccessService.requireApplicationResultEmailManager("google-sub", clubId)).thenReturn(staff);
        when(fixture.generationService.requireGenerationInClubForUpdate(generationId, clubId)).thenReturn(generation);
        when(generation.getId()).thenReturn(generationId);
        when(generation.getClub()).thenReturn(club);
        when(club.getName()).thenReturn("크루캣");
        when(application.getId()).thenReturn(applicationId);
        when(application.getGeneration()).thenReturn(generation);
        when(application.getPerson()).thenReturn(person);
        when(person.getName()).thenReturn("김지원");
        when(person.getDiscordName()).thenReturn(null);
        when(fixture.applicationRepository.findAllByGenerationIdAndStatusForUpdate(
                generationId, ApplicationStatus.ACCEPTED
        )).thenReturn(List.of(application));
        when(fixture.queryService.latestStates(java.util.Set.of(applicationId), ApplicationStatus.ACCEPTED)).thenReturn(Map.of());
        ApplicationResultEmailRequest request = new ApplicationResultEmailRequest(
                generationId,
                ApplicationStatus.ACCEPTED,
                "[{{clubName}}] 합격 안내",
                "{{memberName}}님의 디스코드 이름은 {{discordName}}입니다.",
                null,
                null
        );

        assertThatThrownBy(() -> fixture.service.prepare("google-sub", clubId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("필수 변수");
        verify(fixture.batchRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(fixture.messageRepository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 합격_메일이_전송되면_그때_부원을_생성한다() {
        Fixture fixture = fixture();
        UUID batchId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        ApplicationResultEmailBatch batch = mock(ApplicationResultEmailBatch.class);
        ApplicationResultEmailMessage message = mock(ApplicationResultEmailMessage.class);
        Application application = mock(Application.class);
        Generation generation = mock(Generation.class);
        Person person = mock(Person.class);
        when(fixture.batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(fixture.messageRepository.findAllByBatchId(batchId)).thenReturn(List.of(message));
        when(batch.getDecision()).thenReturn(ApplicationStatus.ACCEPTED);
        when(message.getId()).thenReturn(messageId);
        when(message.getStatus()).thenReturn(ApplicationResultEmailMessageStatus.SENT);
        when(message.getApplication()).thenReturn(application);
        when(application.getGeneration()).thenReturn(generation);
        when(application.getPerson()).thenReturn(person);

        fixture.service.complete(batchId, List.of(EmailSendResult.sent(messageId, "provider-id")));

        verify(fixture.generationMemberService).ensureAcceptedMember(generation, person);
    }

    @Test
    void 불합격_메일이_전송되어도_부원을_생성하지_않는다() {
        Fixture fixture = fixture();
        UUID batchId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        ApplicationResultEmailBatch batch = mock(ApplicationResultEmailBatch.class);
        ApplicationResultEmailMessage message = mock(ApplicationResultEmailMessage.class);
        when(fixture.batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(fixture.messageRepository.findAllByBatchId(batchId)).thenReturn(List.of(message));
        when(batch.getDecision()).thenReturn(ApplicationStatus.REJECTED);
        when(message.getId()).thenReturn(messageId);
        when(message.getStatus()).thenReturn(ApplicationResultEmailMessageStatus.SENT);

        fixture.service.complete(batchId, List.of(EmailSendResult.sent(messageId, "provider-id")));

        verify(fixture.generationMemberService, never()).ensureAcceptedMember(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()
        );
    }

    private Fixture fixture() {
        ApplicationRepository applicationRepository = mock(ApplicationRepository.class);
        ApplicationResultEmailBatchRepository batchRepository = mock(ApplicationResultEmailBatchRepository.class);
        ApplicationResultEmailMessageRepository messageRepository = mock(ApplicationResultEmailMessageRepository.class);
        ApplicationResultEmailQueryService queryService = mock(ApplicationResultEmailQueryService.class);
        ClubAccessService clubAccessService = mock(ClubAccessService.class);
        GenerationService generationService = mock(GenerationService.class);
        GenerationMemberService generationMemberService = mock(GenerationMemberService.class);
        return new Fixture(
                new ApplicationResultEmailPersistenceService(
                        applicationRepository, batchRepository, messageRepository, queryService,
                        new ApplicationResultEmailTemplateRenderer(), clubAccessService,
                        generationService, generationMemberService
                ),
                applicationRepository, batchRepository, messageRepository, queryService,
                clubAccessService, generationService, generationMemberService
        );
    }
}
