package com.clubflow.backend;

import com.clubflow.backend.application.ApplicationAnswerRepository;
import com.clubflow.backend.application.ApplicationRepository;
import com.clubflow.backend.application.ApplicationService;
import com.clubflow.backend.application.ApplicationStatusHistoryRepository;
import com.clubflow.backend.application.ApplicationStatus;
import com.clubflow.backend.application.dto.ApplicationAnswerRequest;
import com.clubflow.backend.application.dto.ApplicationDetailResponse;
import com.clubflow.backend.application.dto.ManualApplicationRequest;
import com.clubflow.backend.application.email.ApplicationResultEmailBatchRepository;
import com.clubflow.backend.application.email.ApplicationResultEmailBatchStatus;
import com.clubflow.backend.application.email.ApplicationResultEmailMessageRepository;
import com.clubflow.backend.application.email.ApplicationResultEmailService;
import com.clubflow.backend.application.email.EmailSendCommand;
import com.clubflow.backend.application.email.EmailSendResult;
import com.clubflow.backend.application.email.EmailSender;
import com.clubflow.backend.application.email.dto.ApplicationResultEmailBatchResponse;
import com.clubflow.backend.application.email.dto.ApplicationResultEmailRequest;
import com.clubflow.backend.club.ClubRepository;
import com.clubflow.backend.club.ClubService;
import com.clubflow.backend.club.ClubStaffRepository;
import com.clubflow.backend.club.ClubStaffRole;
import com.clubflow.backend.club.ClubStaffStatus;
import com.clubflow.backend.club.dto.ClubResponse;
import com.clubflow.backend.club.dto.CreateClubRequest;
import com.clubflow.backend.common.ConflictException;
import com.clubflow.backend.common.InvalidRequestException;
import com.clubflow.backend.generation.GenerationRepository;
import com.clubflow.backend.generation.GenerationService;
import com.clubflow.backend.generation.dto.CreateGenerationRequest;
import com.clubflow.backend.generation.dto.GenerationResponse;
import com.clubflow.backend.member.GenerationMemberRepository;
import com.clubflow.backend.member.GenerationMemberService;
import com.clubflow.backend.member.GenerationMemberStatusHistoryRepository;
import com.clubflow.backend.person.PersonRepository;
import com.clubflow.backend.user.User;
import com.clubflow.backend.user.UserRepository;
import com.clubflow.backend.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({TestcontainersConfiguration.class, ClubFlowIntegrationTests.EmailTestConfiguration.class})
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
class ClubFlowIntegrationTests {

    @Autowired
    UserService userService;

    @Autowired
    ClubService clubService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ClubRepository clubRepository;

    @Autowired
    ClubStaffRepository clubStaffRepository;

    @Autowired
    GenerationService generationService;

    @Autowired
    GenerationRepository generationRepository;

    @Autowired
    ApplicationService applicationService;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    ApplicationAnswerRepository applicationAnswerRepository;

    @Autowired
    PersonRepository personRepository;

    @Autowired
    GenerationMemberRepository generationMemberRepository;

    @Autowired
    GenerationMemberService generationMemberService;

    @Autowired
    GenerationMemberStatusHistoryRepository statusHistoryRepository;

    @Autowired
    ApplicationStatusHistoryRepository applicationStatusHistoryRepository;

    @Autowired
    ApplicationResultEmailService applicationResultEmailService;

    @Autowired
    ApplicationResultEmailBatchRepository applicationResultEmailBatchRepository;

    @Autowired
    ApplicationResultEmailMessageRepository applicationResultEmailMessageRepository;

    @BeforeEach
    void setUp() {
        applicationResultEmailMessageRepository.deleteAll();
        applicationResultEmailBatchRepository.deleteAll();
        statusHistoryRepository.deleteAll();
        applicationStatusHistoryRepository.deleteAll();
        applicationAnswerRepository.deleteAll();
        generationMemberRepository.deleteAll();
        applicationRepository.deleteAll();
        personRepository.deleteAll();
        generationRepository.deleteAll();
        clubStaffRepository.deleteAll();
        clubRepository.deleteAll();
        userRepository.deleteAll();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class EmailTestConfiguration {

        @Bean
        @Primary
        EmailSender testEmailSender() {
            return new EmailSender() {
                @Override
                public boolean isEnabled() {
                    return true;
                }

                @Override
                public List<EmailSendResult> sendBatch(
                        String batchIdempotencyKey,
                        List<EmailSendCommand> commands
                ) {
                    return commands.stream()
                            .map(command -> EmailSendResult.sent(
                                    command.messageId(), "test-provider-" + command.messageId()
                            ))
                            .toList();
                }
            };
        }
    }

    @Test
    void 첫_로그인은_회원을_생성하고_재로그인은_같은_회원을_갱신한다() {
        User firstLogin = userService.synchronizeGoogleUser(
                "google-sub-001",
                "owner@example.com",
                "첫 이름",
                "https://example.com/old.png"
        );
        User secondLogin = userService.synchronizeGoogleUser(
                "google-sub-001",
                "OWNER@example.com",
                "변경된 이름",
                "https://example.com/new.png"
        );

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(secondLogin.getId()).isEqualTo(firstLogin.getId());
        assertThat(secondLogin.getEmail()).isEqualTo("owner@example.com");
        assertThat(secondLogin.getName()).isEqualTo("변경된 이름");
    }

    @Test
    void 동아리_생성자는_승인된_회장_권한을_동시에_받는다() {
        userService.synchronizeGoogleUser(
                "google-sub-001",
                "owner@example.com",
                "회장",
                null
        );

        ClubResponse created = clubService.createClub(
                "google-sub-001",
                new CreateClubRequest("아우내", "테스트 동아리")
        );
        List<ClubResponse> accessibleClubs = clubService.findAccessibleClubs("google-sub-001");

        assertThat(clubRepository.count()).isEqualTo(1);
        assertThat(clubStaffRepository.count()).isEqualTo(1);
        assertThat(created.role()).isEqualTo(ClubStaffRole.PRESIDENT);
        assertThat(created.status()).isEqualTo(ClubStaffStatus.APPROVED);
        assertThat(accessibleClubs).extracting(ClubResponse::id).containsExactly(created.id());
    }

    @Test
    void 결과_메일_전에는_합격과_불합격을_정정할_수_있고_부원은_생기지_않는다() {
        userService.synchronizeGoogleUser(
                "google-sub-001",
                "owner@example.com",
                "회장",
                null
        );
        ClubResponse club = clubService.createClub(
                "google-sub-001",
                new CreateClubRequest("아우내", "테스트 동아리")
        );
        GenerationResponse generation = generationService.create(
                "google-sub-001",
                club.id(),
                new CreateGenerationRequest(
                        "2026-2 학기",
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 12, 31)
                )
        );
        ApplicationDetailResponse application = applicationService.createManual(
                "google-sub-001",
                club.id(),
                new ManualApplicationRequest(
                        generation.id(),
                        "지원자",
                        "APPLICANT@EXAMPLE.COM",
                        "010-0000-0000",
                        "20260001",
                        List.of(new ApplicationAnswerRequest(
                                "motivation",
                                "지원 동기",
                                "백엔드를 공부하고 싶습니다."
                        ))
                )
        );

        applicationService.changeStatus(
                "google-sub-001",
                application.id(),
                ApplicationStatus.ACCEPTED,
                null
        );
        applicationService.changeStatus(
                "google-sub-001",
                application.id(),
                ApplicationStatus.ACCEPTED,
                null
        );

        assertThat(personRepository.findAll())
                .extracting(person -> person.getEmail())
                .containsExactly("applicant@example.com");
        assertThat(generationMemberRepository.count()).isZero();
        ApplicationDetailResponse corrected = applicationService.changeStatus(
                "google-sub-001",
                application.id(),
                ApplicationStatus.REJECTED,
                "결과 정정"
        );
        assertThat(corrected.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(corrected.statusHistory()).hasSize(2);
        assertThat(generationMemberRepository.count()).isZero();

        generationMemberService.ensureAcceptedMember(
                generationRepository.findById(generation.id()).orElseThrow(),
                personRepository.findByClubIdAndEmail(club.id(), "applicant@example.com").orElseThrow()
        );
        assertThatThrownBy(() -> applicationService.changeStatus(
                "google-sub-001", application.id(), ApplicationStatus.ACCEPTED, "기존 데이터 정정"
        )).isInstanceOf(ConflictException.class)
                .hasMessage("이미 부원으로 등록된 기존 지원 결과는 변경할 수 없습니다.");
    }

    @Test
    void 합격_메일은_읽기_전용_트랜잭션에_막히지_않고_발송_결과와_부원을_저장한다() {
        userService.synchronizeGoogleUser(
                "google-sub-001",
                "owner@example.com",
                "회장",
                null
        );
        ClubResponse club = clubService.createClub(
                "google-sub-001",
                new CreateClubRequest("아우내", "테스트 동아리")
        );
        GenerationResponse generation = generationService.create(
                "google-sub-001",
                club.id(),
                new CreateGenerationRequest(
                        "2026-2 학기",
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 12, 31)
                )
        );
        ApplicationDetailResponse application = applicationService.createManual(
                "google-sub-001",
                club.id(),
                new ManualApplicationRequest(
                        generation.id(),
                        "지원자",
                        "applicant@example.com",
                        "010-0000-0000",
                        "20260001",
                        List.of()
                )
        );
        applicationService.changeStatus(
                "google-sub-001",
                application.id(),
                ApplicationStatus.ACCEPTED,
                null
        );

        ApplicationResultEmailBatchResponse result = applicationResultEmailService.send(
                "google-sub-001",
                club.id(),
                new ApplicationResultEmailRequest(
                        generation.id(),
                        ApplicationStatus.ACCEPTED,
                        "[아우내] 합격 안내",
                        "지원자님, 합격을 축하합니다.",
                        null,
                        Set.of(application.id())
                )
        );

        assertThat(result.status()).isEqualTo(ApplicationResultEmailBatchStatus.COMPLETED);
        assertThat(result.sentCount()).isEqualTo(1);
        assertThat(applicationResultEmailBatchRepository.count()).isEqualTo(1);
        assertThat(applicationResultEmailMessageRepository.count()).isEqualTo(1);
        assertThat(generationMemberRepository.count()).isEqualTo(1);
    }

    @Test
    void 지원서를_동시에_합격과_불합격_처리하면_사유없는_후속_정정은_거부한다() throws Exception {
        userService.synchronizeGoogleUser(
                "google-sub-001", "owner@example.com", "회장", null
        );
        ClubResponse club = clubService.createClub(
                "google-sub-001", new CreateClubRequest("아우내", "테스트 동아리")
        );
        GenerationResponse generation = generationService.create(
                "google-sub-001",
                club.id(),
                new CreateGenerationRequest(
                        "2026-2 학기",
                        LocalDate.of(2026, 7, 1),
                        LocalDate.of(2026, 12, 31)
                )
        );
        ApplicationDetailResponse application = applicationService.createManual(
                "google-sub-001",
                club.id(),
                new ManualApplicationRequest(
                        generation.id(),
                        "동시 처리 지원자",
                        "concurrent@example.com",
                        null,
                        "20260002",
                        List.of(new ApplicationAnswerRequest("motivation", "지원 동기", "동시 처리 테스트"))
                )
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<ApplicationStatus> accepted = executor.submit(() -> {
                ready.countDown();
                start.await();
                return applicationService.changeStatus(
                        "google-sub-001", application.id(), ApplicationStatus.ACCEPTED, null
                ).status();
            });
            Future<ApplicationStatus> rejected = executor.submit(() -> {
                ready.countDown();
                start.await();
                return applicationService.changeStatus(
                        "google-sub-001", application.id(), ApplicationStatus.REJECTED, null
                ).status();
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int successCount = 0;
            int rejectedRequestCount = 0;
            for (Future<ApplicationStatus> result : List.of(accepted, rejected)) {
                try {
                    result.get(10, TimeUnit.SECONDS);
                    successCount++;
                } catch (ExecutionException exception) {
                    assertThat(exception.getCause()).isInstanceOf(InvalidRequestException.class);
                    rejectedRequestCount++;
                }
            }

            assertThat(successCount).isEqualTo(1);
            assertThat(rejectedRequestCount).isEqualTo(1);
            ApplicationStatus finalStatus = applicationRepository.findById(application.id())
                    .orElseThrow()
                    .getStatus();
            assertThat(finalStatus).isIn(ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED);
            assertThat(generationMemberRepository.count()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }
}
