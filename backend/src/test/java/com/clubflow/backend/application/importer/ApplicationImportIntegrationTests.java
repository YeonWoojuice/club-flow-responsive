package com.clubflow.backend.application.importer;

import com.clubflow.backend.TestcontainersConfiguration;
import com.clubflow.backend.application.Application;
import com.clubflow.backend.application.ApplicationAnswer;
import com.clubflow.backend.application.ApplicationAnswerRepository;
import com.clubflow.backend.application.ApplicationRepository;
import com.clubflow.backend.application.ApplicationService;
import com.clubflow.backend.application.ApplicationSourceType;
import com.clubflow.backend.application.dto.ApplicationAnswerRequest;
import com.clubflow.backend.application.dto.ManualApplicationRequest;
import com.clubflow.backend.application.importer.dto.ApplicationImportAnswerRequest;
import com.clubflow.backend.application.importer.dto.ApplicationImportApplyResponse;
import com.clubflow.backend.application.importer.dto.ApplicationImportPreviewResponse;
import com.clubflow.backend.application.importer.dto.ApplicationImportRequest;
import com.clubflow.backend.application.importer.dto.ApplicationImportRowRequest;
import com.clubflow.backend.club.ClubRepository;
import com.clubflow.backend.club.ClubService;
import com.clubflow.backend.club.ClubStaffRepository;
import com.clubflow.backend.club.dto.ClubResponse;
import com.clubflow.backend.club.dto.CreateClubRequest;
import com.clubflow.backend.generation.GenerationRepository;
import com.clubflow.backend.generation.GenerationService;
import com.clubflow.backend.generation.GenerationStatus;
import com.clubflow.backend.generation.dto.CreateGenerationRequest;
import com.clubflow.backend.generation.dto.GenerationResponse;
import com.clubflow.backend.generation.dto.UpdateGenerationRequest;
import com.clubflow.backend.member.GenerationMemberRepository;
import com.clubflow.backend.member.GenerationMemberStatusHistoryRepository;
import com.clubflow.backend.person.Person;
import com.clubflow.backend.person.PersonRepository;
import com.clubflow.backend.user.UserRepository;
import com.clubflow.backend.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
class ApplicationImportIntegrationTests {

    @Autowired ApplicationImportService applicationImportService;
    @Autowired ApplicationService applicationService;
    @Autowired UserService userService;
    @Autowired ClubService clubService;
    @Autowired GenerationService generationService;
    @Autowired ApplicationAnswerRepository applicationAnswerRepository;
    @Autowired GenerationMemberRepository generationMemberRepository;
    @Autowired GenerationMemberStatusHistoryRepository statusHistoryRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired PersonRepository personRepository;
    @Autowired GenerationRepository generationRepository;
    @Autowired ClubStaffRepository clubStaffRepository;
    @Autowired ClubRepository clubRepository;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void setUp() {
        statusHistoryRepository.deleteAll();
        applicationAnswerRepository.deleteAll();
        generationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        personRepository.deleteAll();
        generationRepository.deleteAll();
        clubStaffRepository.deleteAll();
        clubRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 구글폼_지원자를_두_번_확정해도_한_번만_저장하고_원본_지원시간과_답변을_보존한다() {
        TestData data = prepareActiveGeneration();
        Instant submittedAt = Instant.parse("2026-07-01T03:04:05Z");
        ApplicationImportRequest request = request(
                data.generation().id(), "new@example.com", "새 지원자", "20260001", submittedAt
        );

        ApplicationImportPreviewResponse preview = applicationImportService.preview(
                "google-sub-001", data.club().id(), request
        );
        ApplicationImportApplyResponse first = applicationImportService.apply(
                "google-sub-001", data.club().id(), request
        );
        ApplicationImportApplyResponse second = applicationImportService.apply(
                "google-sub-001", data.club().id(), request
        );

        Person person = personRepository.findByClubIdAndEmail(data.club().id(), "new@example.com").orElseThrow();
        Application application = applicationRepository
                .findAllByGenerationIdAndPersonIdIn(data.generation().id(), java.util.Set.of(person.getId()))
                .getFirst();
        List<ApplicationAnswer> answers = applicationAnswerRepository
                .findAllByApplicationIdOrderByDisplayOrderAsc(application.getId());

        assertThat(preview.readyCount()).isEqualTo(1);
        assertThat(first.createdCount()).isEqualTo(1);
        assertThat(second.createdCount()).isZero();
        assertThat(second.skippedCount()).isEqualTo(1);
        assertThat(application.getSourceType()).isEqualTo(ApplicationSourceType.GOOGLE_FORM);
        assertThat(application.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(answers).extracting(ApplicationAnswer::getQuestionKey, ApplicationAnswer::getAnswerValue)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("motivation", "지원 동기 답변"),
                        org.assertj.core.groups.Tuple.tuple("experience", "활동 경험 답변")
                );
    }

    @Test
    void 기존_인물을_재사용할_때_이름과_학번을_새_지원서_값으로_덮어쓰지_않는다() {
        TestData data = prepareActiveGenerationWithExistingPerson();
        ApplicationImportRequest request = request(
                data.generation().id(), "member@example.com", "바뀐 이름", "99999999",
                Instant.parse("2026-07-02T03:04:05Z")
        );

        ApplicationImportApplyResponse result = applicationImportService.apply(
                "google-sub-001", data.club().id(), request
        );

        Person person = personRepository.findByClubIdAndEmail(data.club().id(), "member@example.com").orElseThrow();
        assertThat(result.createdCount()).isEqualTo(1);
        assertThat(person.getName()).isEqualTo("기존 이름");
        assertThat(person.getStudentNumber()).isEqualTo("20230001");
        assertThat(personRepository.count()).isEqualTo(1);
    }

    private TestData prepareActiveGeneration() {
        userService.synchronizeGoogleUser("google-sub-001", "owner@example.com", "회장", null);
        ClubResponse club = clubService.createClub(
                "google-sub-001", new CreateClubRequest("아우내", "테스트 동아리")
        );
        GenerationResponse generation = generationService.create(
                "google-sub-001", club.id(),
                new CreateGenerationRequest(
                        "2026-2 학기", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31)
                )
        );
        return new TestData(club, generation);
    }

    private TestData prepareActiveGenerationWithExistingPerson() {
        TestData previous = prepareActiveGeneration();
        applicationService.createManual(
                "google-sub-001", previous.club().id(),
                new ManualApplicationRequest(
                        previous.generation().id(), "기존 이름", "member@example.com",
                        "010-1111-1111", "20230001",
                        List.of(new ApplicationAnswerRequest("old", "기존 질문", "기존 답변"))
                )
        );
        generationService.update(
                "google-sub-001", previous.generation().id(),
                new UpdateGenerationRequest(
                        previous.generation().name(), previous.generation().startDate(),
                        previous.generation().endDate(), GenerationStatus.CLOSED
                )
        );
        GenerationResponse target = generationService.create(
                "google-sub-001", previous.club().id(),
                new CreateGenerationRequest(
                        "2027-1 학기", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 6, 30)
                )
        );
        return new TestData(previous.club(), target);
    }

    private ApplicationImportRequest request(
            java.util.UUID generationId,
            String email,
            String name,
            String studentNumber,
            Instant submittedAt
    ) {
        return new ApplicationImportRequest(
                generationId,
                List.of(new ApplicationImportRowRequest(
                        2, name, email, "010-0000-0000", studentNumber, submittedAt,
                        List.of(
                                new ApplicationImportAnswerRequest(
                                        "motivation", "지원 동기", "지원 동기 답변"
                                ),
                                new ApplicationImportAnswerRequest(
                                        "experience", "활동 경험", "활동 경험 답변"
                                )
                        )
                ))
        );
    }

    private record TestData(ClubResponse club, GenerationResponse generation) {
    }
}
