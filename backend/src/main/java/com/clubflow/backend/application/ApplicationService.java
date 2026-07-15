package com.clubflow.backend.application;

import com.clubflow.backend.application.dto.ApplicationAnswerRequest;
import com.clubflow.backend.application.dto.ApplicationDetailResponse;
import com.clubflow.backend.application.dto.ApplicationSummaryResponse;
import com.clubflow.backend.application.dto.ManualApplicationRequest;
import com.clubflow.backend.application.dto.ApplicationStatusHistoryResponse;
import com.clubflow.backend.application.email.ApplicationResultEmailQueryService;
import com.clubflow.backend.application.email.ApplicationResultEmailStatus;
import com.clubflow.backend.club.Club;
import com.clubflow.backend.club.ClubAccessService;
import com.clubflow.backend.common.ConflictException;
import com.clubflow.backend.common.NotFoundException;
import com.clubflow.backend.common.InvalidRequestException;
import com.clubflow.backend.generation.Generation;
import com.clubflow.backend.generation.GenerationService;
import com.clubflow.backend.generation.GenerationStatus;
import com.clubflow.backend.member.GenerationMember;
import com.clubflow.backend.member.GenerationMemberRepository;
import com.clubflow.backend.person.Person;
import com.clubflow.backend.person.PersonService;
import com.clubflow.backend.user.User;
import com.clubflow.backend.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationAnswerRepository applicationAnswerRepository;
    private final ClubAccessService clubAccessService;
    private final GenerationService generationService;
    private final PersonService personService;
    private final ApplicationResultEmailQueryService resultEmailQueryService;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final GenerationMemberRepository generationMemberRepository;
    private final UserService userService;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            ApplicationAnswerRepository applicationAnswerRepository,
            ClubAccessService clubAccessService,
            GenerationService generationService,
            PersonService personService,
            ApplicationResultEmailQueryService resultEmailQueryService,
            ApplicationStatusHistoryRepository statusHistoryRepository,
            GenerationMemberRepository generationMemberRepository,
            UserService userService
    ) {
        this.applicationRepository = applicationRepository;
        this.applicationAnswerRepository = applicationAnswerRepository;
        this.clubAccessService = clubAccessService;
        this.generationService = generationService;
        this.personService = personService;
        this.resultEmailQueryService = resultEmailQueryService;
        this.statusHistoryRepository = statusHistoryRepository;
        this.generationMemberRepository = generationMemberRepository;
        this.userService = userService;
    }

    public List<ApplicationSummaryResponse> list(String googleSub, UUID clubId) {
        clubAccessService.requireAccessibleClub(googleSub, clubId);
        List<Application> applications = applicationRepository.findAllByClubId(clubId);
        Map<UUID, ApplicationResultEmailQueryService.ResultEmailState> states = resultEmailQueryService.latestStates(
                applications.stream().collect(Collectors.toMap(Application::getId, Application::getStatus))
        );
        Map<MemberKey, GenerationMember> members = generationMemberRepository.findAllByClubId(clubId).stream()
                .collect(Collectors.toMap(
                        member -> new MemberKey(member.getGeneration().getId(), member.getPerson().getId()),
                        Function.identity()
                ));
        return applications.stream()
                .map(application -> ApplicationSummaryResponse.from(
                        application,
                        states.getOrDefault(application.getId(), ApplicationResultEmailQueryService.ResultEmailState.notSent()),
                        members.get(new MemberKey(application.getGeneration().getId(), application.getPerson().getId()))
                ))
                .toList();
    }

    @Transactional
    public ApplicationDetailResponse createManual(
            String googleSub,
            UUID clubId,
            ManualApplicationRequest request
    ) {
        Club club = clubAccessService.requireAccessibleClub(googleSub, clubId);
        Generation generation = generationService.requireGenerationInClub(request.generationId(), clubId);
        if (generation.getStatus() != GenerationStatus.ACTIVE) {
            throw new ConflictException("종료된 학기에는 지원자를 등록할 수 없습니다.");
        }
        validateUniqueQuestionKeys(request.applicationAnswers());
        Person person = personService.findOrCreate(
                club,
                request.name(),
                request.email(),
                request.phone(),
                request.studentNumber(),
                null
        );
        if (applicationRepository.existsByGenerationIdAndPersonId(generation.getId(), person.getId())) {
            throw new ConflictException("같은 학기에 이미 등록된 지원자가 있습니다.");
        }

        Application application = applicationRepository.save(Application.createManual(generation, person));
        List<ApplicationAnswer> answers = createAnswers(application, request.applicationAnswers());
        applicationAnswerRepository.saveAll(answers);
        ApplicationResultEmailQueryService.ResultEmailState state = resultEmailQueryService.latestStates(
                Set.of(application.getId()), application.getStatus()
        ).getOrDefault(application.getId(), ApplicationResultEmailQueryService.ResultEmailState.notSent());
        return ApplicationDetailResponse.from(application, answers, state, null, List.of());
    }

    public ApplicationDetailResponse get(String googleSub, UUID applicationId) {
        Application application = getApplication(applicationId);
        clubAccessService.requireAccessibleClub(
                googleSub,
                application.getGeneration().getClub().getId()
        );
        return detail(application);
    }

    @Transactional
    public ApplicationDetailResponse changeStatus(
            String googleSub,
            UUID applicationId,
            ApplicationStatus targetStatus,
            String reason
    ) {
        Application application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new NotFoundException("지원서를 찾을 수 없습니다."));
        clubAccessService.requireAccessibleClub(
                googleSub,
                application.getGeneration().getClub().getId()
        );
        ApplicationStatus previousStatus = application.getStatus();
        if (previousStatus == targetStatus) {
            return detail(application);
        }
        validateResultCanChange(application);
        String normalizedReason = normalizeReason(reason);
        if (isDecisionCorrection(previousStatus, targetStatus)) {
            if (generationMemberRepository.findByGenerationIdAndPersonId(
                    application.getGeneration().getId(), application.getPerson().getId()
            ).isPresent()) {
                throw new ConflictException("이미 부원으로 등록된 기존 지원 결과는 변경할 수 없습니다.");
            }
            if (normalizedReason == null) {
                throw new InvalidRequestException("합격·불합격 결과를 정정하는 사유를 입력해 주세요.");
            }
        }
        application.changeStatus(targetStatus);
        User changedBy = userService.getByGoogleSub(googleSub);
        statusHistoryRepository.save(ApplicationStatusHistory.create(
                application, previousStatus, targetStatus, normalizedReason, changedBy
        ));
        return detail(application);
    }

    private Application getApplication(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("지원서를 찾을 수 없습니다."));
    }

    private ApplicationDetailResponse detail(Application application) {
        List<ApplicationAnswer> answers =
                applicationAnswerRepository.findAllByApplicationIdOrderByDisplayOrderAsc(application.getId());
        ApplicationResultEmailQueryService.ResultEmailState state = resultEmailQueryService.latestStates(
                Set.of(application.getId()), application.getStatus()
        ).getOrDefault(application.getId(), ApplicationResultEmailQueryService.ResultEmailState.notSent());
        GenerationMember member = generationMemberRepository.findByGenerationIdAndPersonId(
                application.getGeneration().getId(), application.getPerson().getId()
        ).orElse(null);
        List<ApplicationStatusHistoryResponse> history = statusHistoryRepository
                .findAllByApplicationIdOrderByChangedAtDesc(application.getId())
                .stream()
                .map(ApplicationStatusHistoryResponse::from)
                .toList();
        return ApplicationDetailResponse.from(application, answers, state, member, history);
    }

    private void validateResultCanChange(Application application) {
        if (application.getStatus() != ApplicationStatus.ACCEPTED
                && application.getStatus() != ApplicationStatus.REJECTED) {
            return;
        }
        ApplicationResultEmailStatus emailStatus = resultEmailQueryService.latestStates(
                Set.of(application.getId()), application.getStatus()
        ).getOrDefault(
                application.getId(), ApplicationResultEmailQueryService.ResultEmailState.notSent()
        ).status();
        if (emailStatus != ApplicationResultEmailStatus.NOT_SENT
                && emailStatus != ApplicationResultEmailStatus.FAILED) {
            throw new ConflictException("결과 메일 발송을 시작했거나 완료한 지원 결과는 변경할 수 없습니다.");
        }
    }

    private boolean isDecisionCorrection(ApplicationStatus previousStatus, ApplicationStatus targetStatus) {
        return (previousStatus == ApplicationStatus.ACCEPTED && targetStatus == ApplicationStatus.REJECTED)
                || (previousStatus == ApplicationStatus.REJECTED && targetStatus == ApplicationStatus.ACCEPTED);
    }

    private String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }

    private List<ApplicationAnswer> createAnswers(
            Application application,
            List<ApplicationAnswerRequest> answerRequests
    ) {
        return java.util.stream.IntStream.range(0, answerRequests.size())
                .mapToObj(index -> {
                    ApplicationAnswerRequest answer = answerRequests.get(index);
                    return ApplicationAnswer.createText(
                            application,
                            answer.questionKey(),
                            answer.questionLabel(),
                            answer.answerValue(),
                            index
                    );
                })
                .toList();
    }

    private void validateUniqueQuestionKeys(List<ApplicationAnswerRequest> answers) {
        Set<String> keys = new HashSet<>();
        boolean duplicated = answers.stream()
                .map(answer -> answer.questionKey().trim())
                .anyMatch(key -> !keys.add(key));
        if (duplicated) {
            throw new ConflictException("지원서 질문 키는 중복될 수 없습니다.");
        }
    }

    private record MemberKey(UUID generationId, UUID personId) {
    }
}
