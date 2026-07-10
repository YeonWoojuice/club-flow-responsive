package com.clubflow.backend.member;

import com.clubflow.backend.TestcontainersConfiguration;
import com.clubflow.backend.application.ApplicationAnswerRepository;
import com.clubflow.backend.application.ApplicationRepository;
import com.clubflow.backend.club.Club;
import com.clubflow.backend.club.ClubRepository;
import com.clubflow.backend.club.ClubService;
import com.clubflow.backend.club.ClubStaffRepository;
import com.clubflow.backend.club.dto.ClubResponse;
import com.clubflow.backend.club.dto.CreateClubRequest;
import com.clubflow.backend.common.ConflictException;
import com.clubflow.backend.common.ForbiddenException;
import com.clubflow.backend.common.InvalidRequestException;
import com.clubflow.backend.generation.Generation;
import com.clubflow.backend.generation.GenerationRepository;
import com.clubflow.backend.generation.GenerationService;
import com.clubflow.backend.generation.dto.CreateGenerationRequest;
import com.clubflow.backend.generation.dto.GenerationResponse;
import com.clubflow.backend.member.dto.ChangeGenerationMemberStatusRequest;
import com.clubflow.backend.member.dto.GenerationMemberStatusHistoryResponse;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
class GenerationMemberStatusIntegrationTests {

    @Autowired GenerationMemberService generationMemberService;
    @Autowired GenerationMemberStatusHistoryRepository statusHistoryRepository;
    @Autowired ApplicationAnswerRepository applicationAnswerRepository;
    @Autowired ApplicationRepository applicationRepository;
    @Autowired GenerationMemberRepository generationMemberRepository;
    @Autowired PersonRepository personRepository;
    @Autowired GenerationRepository generationRepository;
    @Autowired ClubStaffRepository clubStaffRepository;
    @Autowired ClubRepository clubRepository;
    @Autowired UserRepository userRepository;
    @Autowired UserService userService;
    @Autowired ClubService clubService;
    @Autowired GenerationService generationService;

    @BeforeEach
    void setUp() {
        statusHistoryRepository.deleteAll();
        generationMemberRepository.deleteAll();
        applicationAnswerRepository.deleteAll();
        applicationRepository.deleteAll();
        personRepository.deleteAll();
        generationRepository.deleteAll();
        clubStaffRepository.deleteAll();
        clubRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 상태를_변경하면_사유와_변경자가_이력에_저장된다() {
        TestData data = prepareMember();

        generationMemberService.changeStatus(
                data.googleSub(),
                data.member().getId(),
                new ChangeGenerationMemberStatusRequest(GenerationMemberStatus.INACTIVE, "  군 복무  ")
        );
        generationMemberService.changeStatus(
                data.googleSub(),
                data.member().getId(),
                new ChangeGenerationMemberStatusRequest(GenerationMemberStatus.ACTIVE, "복학")
        );

        GenerationMember saved = generationMemberRepository.findById(data.member().getId()).orElseThrow();
        List<GenerationMemberStatusHistoryResponse> histories = generationMemberService.getStatusHistory(
                data.googleSub(), data.member().getId()
        );
        assertThat(saved.getStatus()).isEqualTo(GenerationMemberStatus.ACTIVE);
        assertThat(histories).hasSize(2);
        assertThat(histories.get(1).previousStatus()).isEqualTo(GenerationMemberStatus.ACTIVE);
        assertThat(histories.get(1).newStatus()).isEqualTo(GenerationMemberStatus.INACTIVE);
        assertThat(histories.get(1).reason()).isEqualTo("군 복무");
        assertThat(histories.get(1).changedByName()).isEqualTo("회장");
        assertThat(histories.get(1).changedByUserId()).isNotNull();
        assertThat(histories.get(1).changedAt()).isNotNull();
    }

    @Test
    void 같은_상태를_다시_요청하면_이력을_추가하지_않는다() {
        TestData data = prepareMember();

        generationMemberService.changeStatus(
                data.googleSub(),
                data.member().getId(),
                new ChangeGenerationMemberStatusRequest(GenerationMemberStatus.ACTIVE, null)
        );

        assertThat(statusHistoryRepository.count()).isZero();
        assertThat(generationMemberRepository.findById(data.member().getId()).orElseThrow().getStatus())
                .isEqualTo(GenerationMemberStatus.ACTIVE);
    }

    @Test
    void 탈퇴에는_사유가_필수이고_탈퇴_후에는_되돌릴_수_없다() {
        TestData data = prepareMember();

        assertThatThrownBy(() -> generationMemberService.changeStatus(
                data.googleSub(),
                data.member().getId(),
                new ChangeGenerationMemberStatusRequest(GenerationMemberStatus.WITHDRAWN, "  ")
        )).isInstanceOf(InvalidRequestException.class);
        assertThat(statusHistoryRepository.count()).isZero();

        generationMemberService.changeStatus(
                data.googleSub(),
                data.member().getId(),
                new ChangeGenerationMemberStatusRequest(GenerationMemberStatus.WITHDRAWN, "개인 사정")
        );

        assertThatThrownBy(() -> generationMemberService.changeStatus(
                data.googleSub(),
                data.member().getId(),
                new ChangeGenerationMemberStatusRequest(GenerationMemberStatus.ACTIVE, "복귀")
        )).isInstanceOf(ConflictException.class);
        assertThat(statusHistoryRepository.count()).isEqualTo(1);
        assertThat(generationMemberRepository.findById(data.member().getId()).orElseThrow().getStatus())
                .isEqualTo(GenerationMemberStatus.WITHDRAWN);
    }

    @Test
    void 승인된_운영진이_아니면_다른_동아리_부원_상태를_변경할_수_없다() {
        TestData data = prepareMember();
        userService.synchronizeGoogleUser("other-google-sub", "other@example.com", "외부인", null);

        assertThatThrownBy(() -> generationMemberService.changeStatus(
                "other-google-sub",
                data.member().getId(),
                new ChangeGenerationMemberStatusRequest(GenerationMemberStatus.INACTIVE, null)
        )).isInstanceOf(ForbiddenException.class);

        assertThat(statusHistoryRepository.count()).isZero();
        assertThat(generationMemberRepository.findById(data.member().getId()).orElseThrow().getStatus())
                .isEqualTo(GenerationMemberStatus.ACTIVE);

        assertThatThrownBy(() -> generationMemberService.getStatusHistory(
                "other-google-sub", data.member().getId()
        )).isInstanceOf(ForbiddenException.class);
    }

    private TestData prepareMember() {
        String googleSub = "member-status-owner";
        userService.synchronizeGoogleUser(googleSub, "owner@example.com", "회장", null);
        ClubResponse clubResponse = clubService.createClub(
                googleSub, new CreateClubRequest("테스트 동아리", null)
        );
        GenerationResponse generationResponse = generationService.create(
                googleSub,
                clubResponse.id(),
                new CreateGenerationRequest(
                        "2026-1 학기",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 6, 30)
                )
        );
        Club club = clubRepository.findById(clubResponse.id()).orElseThrow();
        Generation generation = generationRepository.findById(generationResponse.id()).orElseThrow();
        Person person = personRepository.save(Person.create(
                club, "김민수", "member@example.com", null, "20230001"
        ));
        GenerationMember member = generationMemberRepository.save(
                GenerationMember.createFromAcceptedApplication(generation, person)
        );
        return new TestData(googleSub, member);
    }

    private record TestData(String googleSub, GenerationMember member) {
    }
}
