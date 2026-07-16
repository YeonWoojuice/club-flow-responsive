package com.clubflow.backend.dues;

import com.clubflow.backend.TestcontainersConfiguration;
import com.clubflow.backend.club.*;
import com.clubflow.backend.club.dto.*;
import com.clubflow.backend.dues.dto.*;
import com.clubflow.backend.generation.*;
import com.clubflow.backend.generation.dto.*;
import com.clubflow.backend.member.*;
import com.clubflow.backend.person.*;
import com.clubflow.backend.user.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
class DuesIntegrationTests {
    @Autowired DuesService duesService;
    @Autowired DuesRefundRepository refundRepository;
    @Autowired DuesPaymentRepository paymentRepository;
    @Autowired MemberDueRepository memberDueRepository;
    @Autowired GenerationDuesRefundRuleRepository ruleRepository;
    @Autowired GenerationDuesPolicyRepository policyRepository;
    @Autowired GenerationMemberRepository memberRepository;
    @Autowired GenerationMemberStatusHistoryRepository memberStatusHistoryRepository;
    @Autowired PersonRepository personRepository;
    @Autowired GenerationRepository generationRepository;
    @Autowired ClubStaffRepository clubStaffRepository;
    @Autowired ClubRepository clubRepository;
    @Autowired UserRepository userRepository;
    @Autowired UserService userService;
    @Autowired ClubService clubService;
    @Autowired GenerationService generationService;
    @Autowired GenerationMemberService memberService;

    @BeforeEach void setUp() { clearData(); }
    @AfterEach void tearDown() { clearData(); }

    private void clearData() {
        refundRepository.deleteAll(); paymentRepository.deleteAll(); memberDueRepository.deleteAll();
        ruleRepository.deleteAll(); policyRepository.deleteAll(); memberStatusHistoryRepository.deleteAll(); memberRepository.deleteAll();
        personRepository.deleteAll(); generationRepository.deleteAll(); clubStaffRepository.deleteAll();
        clubRepository.deleteAll(); userRepository.deleteAll();
    }

    @Test void 납부와_환불은_중복되지_않고_환불액의_소수점은_버린다() {
        TestData data = prepare();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        DuesOverviewResponse created = duesService.createPolicy(data.googleSub, data.clubId, data.generationId,
                new CreateDuesPolicyRequest("30001", LocalDate.of(2027, 3, 10), List.of(
                        new RefundRuleRequest("전액 환불", today.minusDays(1), 10000),
                        new RefundRuleRequest("절반 환불", today.plusDays(1), 5000))));
        UUID dueId = created.members().getFirst().memberDueId();
        UUID paymentKey = UUID.randomUUID();
        duesService.recordPayment(data.googleSub, dueId,
                new RecordDuesPaymentRequest(LocalDate.of(2027, 3, 1), paymentKey));
        DuesOverviewResponse duplicatePayment = duesService.recordPayment(data.googleSub, dueId,
                new RecordDuesPaymentRequest(LocalDate.of(2027, 3, 1), paymentKey));
        assertThat(duplicatePayment.totalPaid()).isEqualTo("30001");
        assertThat(paymentRepository.count()).isEqualTo(1);

        memberService.changeStatus(data.googleSub, data.memberId,
                new com.clubflow.backend.member.dto.ChangeGenerationMemberStatusRequest(GenerationMemberStatus.INACTIVE, "활동 중단"));
        memberService.changeStatus(data.googleSub, data.memberId,
                new com.clubflow.backend.member.dto.ChangeGenerationMemberStatusRequest(GenerationMemberStatus.WITHDRAWN, "탈퇴"));
        DuesRefundQuoteResponse quote = duesService.quoteRefund(data.googleSub, dueId);
        assertThat(quote.refundAmount()).isEqualTo("15000");

        UUID refundKey = UUID.randomUUID();
        duesService.recordRefund(data.googleSub, dueId,
                new RecordDuesRefundRequest(refundKey));
        DuesOverviewResponse duplicateRefund = duesService.recordRefund(data.googleSub, dueId,
                new RecordDuesRefundRequest(refundKey));
        assertThat(duplicateRefund.totalRefunded()).isEqualTo("15000");
        assertThat(refundRepository.count()).isEqualTo(1);

        DuesOverviewResponse refundCanceled = duesService.cancelRefund(data.googleSub, dueId,
                new CancelDuesRecordRequest("환불 입력 오류"));
        assertThat(refundCanceled.totalRefunded()).isEqualTo("0");
        DuesOverviewResponse paymentCanceled = duesService.cancelPayment(data.googleSub, dueId,
                new CancelDuesRecordRequest("납부 입력 오류"));
        assertThat(paymentCanceled.totalPaid()).isEqualTo("0");
        assertThat(paymentCanceled.unpaidCount()).isEqualTo(1);
    }

    private TestData prepare() {
        String sub = "dues-owner";
        userService.synchronizeGoogleUser(sub, "dues-owner@example.com", "회장", null);
        ClubResponse club = clubService.createClub(sub, new CreateClubRequest("회비 동아리", null));
        GenerationResponse generation = generationService.create(sub, club.id(),
                new CreateGenerationRequest("2027-1", LocalDate.of(2027, 1, 1), LocalDate.of(2027, 6, 30)));
        Club clubEntity = clubRepository.findById(club.id()).orElseThrow();
        Generation generationEntity = generationRepository.findById(generation.id()).orElseThrow();
        Person person = personRepository.save(Person.create(clubEntity, "김부원", "dues-member@example.com", null, "20270001"));
        GenerationMember member = memberRepository.save(GenerationMember.createFromAcceptedApplication(generationEntity, person));
        return new TestData(sub, club.id(), generation.id(), member.getId());
    }

    private record TestData(String googleSub, UUID clubId, UUID generationId, UUID memberId) {}
}
