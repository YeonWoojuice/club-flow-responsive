package com.clubflow.backend.dues;

import com.clubflow.backend.club.ClubAccessService;
import com.clubflow.backend.common.ConflictException;
import com.clubflow.backend.common.InvalidRequestException;
import com.clubflow.backend.common.NotFoundException;
import com.clubflow.backend.dues.dto.*;
import com.clubflow.backend.generation.Generation;
import com.clubflow.backend.generation.GenerationRepository;
import com.clubflow.backend.member.*;
import com.clubflow.backend.user.User;
import com.clubflow.backend.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DuesService {
    private final GenerationDuesPolicyRepository policyRepository;
    private final GenerationDuesRefundRuleRepository ruleRepository;
    private final MemberDueRepository memberDueRepository;
    private final DuesPaymentRepository paymentRepository;
    private final DuesRefundRepository refundRepository;
    private final GenerationRepository generationRepository;
    private final GenerationMemberRepository memberRepository;
    private final GenerationMemberStatusHistoryRepository memberStatusHistoryRepository;
    private final ClubAccessService clubAccessService;
    private final UserService userService;

    public DuesService(GenerationDuesPolicyRepository policyRepository,
                       GenerationDuesRefundRuleRepository ruleRepository,
                       MemberDueRepository memberDueRepository,
                       DuesPaymentRepository paymentRepository,
                       DuesRefundRepository refundRepository,
                       GenerationRepository generationRepository,
                       GenerationMemberRepository memberRepository,
                       GenerationMemberStatusHistoryRepository memberStatusHistoryRepository,
                       ClubAccessService clubAccessService,
                       UserService userService) {
        this.policyRepository = policyRepository; this.ruleRepository = ruleRepository;
        this.memberDueRepository = memberDueRepository; this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository; this.generationRepository = generationRepository;
        this.memberRepository = memberRepository; this.memberStatusHistoryRepository = memberStatusHistoryRepository;
        this.clubAccessService = clubAccessService; this.userService = userService;
    }

    @Transactional
    public DuesOverviewResponse createPolicy(String googleSub, UUID clubId, UUID generationId, CreateDuesPolicyRequest request) {
        clubAccessService.requireAccessibleClub(googleSub, clubId);
        Generation generation = generationRepository.findByIdAndClubId(generationId, clubId)
                .orElseThrow(() -> new NotFoundException("해당 동아리의 학기를 찾을 수 없습니다."));
        if (policyRepository.existsByGenerationId(generationId)) {
            throw new ConflictException("이미 이 학기의 회비가 설정되어 있습니다.");
        }
        List<RefundRuleRequest> rules = validateRules(request.refundRules());
        User user = userService.getByGoogleSub(googleSub);
        GenerationDuesPolicy policy = policyRepository.save(GenerationDuesPolicy.create(
                generation, MoneyAmounts.parsePositive(request.amount(), "회비"), request.dueDate(), user));
        for (int index = 0; index < rules.size(); index++) {
            RefundRuleRequest rule = rules.get(index);
            ruleRepository.save(GenerationDuesRefundRule.create(
                    policy, rule.label().trim(), rule.endsOn(), rule.refundRateBps(), index));
        }
        for (GenerationMember member : memberRepository.findAllByGenerationIdWithPerson(generationId)) {
            createAssessment(policy, member, user);
        }
        return overview(googleSub, clubId, generationId);
    }

    public DuesOverviewResponse overview(String googleSub, UUID clubId, UUID generationId) {
        clubAccessService.requireAccessibleClub(googleSub, clubId);
        generationRepository.findByIdAndClubId(generationId, clubId)
                .orElseThrow(() -> new NotFoundException("해당 동아리의 학기를 찾을 수 없습니다."));
        Optional<GenerationDuesPolicy> optionalPolicy = policyRepository.findByGenerationId(generationId);
        if (optionalPolicy.isEmpty()) {
            return new DuesOverviewResponse(null, null, null, List.of(), "0", "0", "0", 0, List.of());
        }
        GenerationDuesPolicy policy = optionalPolicy.get();
        List<MemberDue> dues = memberDueRepository.findAllForOverview(policy.getId());
        Map<UUID, DuesPayment> payments = paymentRepository.findAllByMemberDuePolicyId(policy.getId()).stream()
                .filter(payment -> payment.getCanceledAt() == null)
                .collect(Collectors.toMap(payment -> payment.getMemberDue().getId(), Function.identity()));
        Map<UUID, DuesRefund> refunds = refundRepository.findAllByMemberDuePolicyId(policy.getId()).stream()
                .filter(refund -> refund.getCanceledAt() == null)
                .collect(Collectors.toMap(refund -> refund.getMemberDue().getId(), Function.identity()));
        BigDecimal totalAssessed = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalRefunded = BigDecimal.ZERO;
        int unpaidCount = 0;
        List<DuesMemberRowResponse> rows = new ArrayList<>();
        for (MemberDue due : dues) {
            DuesPayment payment = payments.get(due.getId());
            DuesRefund refund = refunds.get(due.getId());
            BigDecimal paid = payment == null ? BigDecimal.ZERO : payment.getAmount();
            BigDecimal refunded = refund == null ? BigDecimal.ZERO : refund.getAmount();
            if (!due.isExempted()) totalAssessed = totalAssessed.add(due.getAssessedAmount());
            totalPaid = totalPaid.add(paid);
            totalRefunded = totalRefunded.add(refunded);
            if (!due.isExempted() && payment == null) unpaidCount++;
            rows.add(toRow(due, payment, refund, paid, refunded));
        }
        List<DuesRefundRuleResponse> ruleResponses = ruleRepository.findAllByPolicyIdOrderByEndsOnAsc(policy.getId())
                .stream().map(rule -> new DuesRefundRuleResponse(rule.getLabel(), rule.getEndsOn(), rule.getRefundRateBps())).toList();
        return new DuesOverviewResponse(policy.getId(), MoneyAmounts.format(policy.getAmount()), policy.getDueDate(),
                ruleResponses, MoneyAmounts.format(totalAssessed), MoneyAmounts.format(totalPaid),
                MoneyAmounts.format(totalRefunded), unpaidCount, rows);
    }

    @Transactional
    public DuesOverviewResponse recordPayment(String googleSub, UUID dueId, RecordDuesPaymentRequest request) {
        Optional<DuesPayment> duplicate = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (duplicate.isPresent()) {
            if (!duplicate.get().getMemberDue().getId().equals(dueId)) {
                throw new ConflictException("중복 방지 키가 다른 납부 기록에 사용되었습니다.");
            }
            return overviewForDue(googleSub, duplicate.get().getMemberDue());
        }
        MemberDue due = requireDueForUpdate(googleSub, dueId);
        if (due.isExempted()) throw new ConflictException("면제된 부원에게는 납부를 기록할 수 없습니다.");
        if (paymentRepository.findFirstByMemberDueIdAndCanceledAtIsNull(dueId).isPresent()) {
            throw new ConflictException("이미 납부가 확인된 부원입니다.");
        }
        User user = userService.getByGoogleSub(googleSub);
        paymentRepository.save(DuesPayment.create(due, due.getAssessedAmount(), request.paidOn(),
                DuesPaymentSource.ACTUAL, request.idempotencyKey(), user));
        due.getGenerationMember().changeDuesStatus(GenerationMemberDuesStatus.PAID, user);
        return overviewForDue(googleSub, due);
    }

    @Transactional
    public DuesOverviewResponse changeExemption(String googleSub, UUID dueId, ChangeDuesExemptionRequest request) {
        MemberDue due = requireDueForUpdate(googleSub, dueId);
        if (paymentRepository.findFirstByMemberDueIdAndCanceledAtIsNull(dueId).isPresent()
                || refundRepository.findFirstByMemberDueIdAndCanceledAtIsNull(dueId).isPresent()) {
            throw new ConflictException("납부 또는 환불 기록이 있으면 면제 상태를 변경할 수 없습니다.");
        }
        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        if (request.exempted() && reason == null) {
            throw new InvalidRequestException("면제 사유를 입력해 주세요.");
        }
        User user = userService.getByGoogleSub(googleSub);
        due.changeExemption(request.exempted(), request.exempted() ? reason : null);
        due.getGenerationMember().changeDuesStatus(
                request.exempted() ? GenerationMemberDuesStatus.EXEMPT : GenerationMemberDuesStatus.UNPAID, user);
        return overviewForDue(googleSub, due);
    }

    @Transactional
    public DuesOverviewResponse cancelPayment(String googleSub, UUID dueId, CancelDuesRecordRequest request) {
        MemberDue due = requireDueForUpdate(googleSub, dueId);
        if (refundRepository.findFirstByMemberDueIdAndCanceledAtIsNull(dueId).isPresent()) {
            throw new ConflictException("환불 기록을 먼저 취소해 주세요.");
        }
        DuesPayment payment = paymentRepository.findFirstByMemberDueIdAndCanceledAtIsNull(dueId)
                .orElseThrow(() -> new ConflictException("취소할 납부 기록이 없습니다."));
        User user = userService.getByGoogleSub(googleSub);
        payment.cancel(user, request.reason().trim());
        due.getGenerationMember().changeDuesStatus(GenerationMemberDuesStatus.UNPAID, user);
        return overviewForDue(googleSub, due);
    }

    @Transactional
    public DuesOverviewResponse cancelRefund(String googleSub, UUID dueId, CancelDuesRecordRequest request) {
        MemberDue due = requireDueForUpdate(googleSub, dueId);
        DuesRefund refund = refundRepository.findFirstByMemberDueIdAndCanceledAtIsNull(dueId)
                .orElseThrow(() -> new ConflictException("취소할 환불 기록이 없습니다."));
        refund.cancel(userService.getByGoogleSub(googleSub), request.reason().trim());
        return overviewForDue(googleSub, due);
    }

    public DuesRefundQuoteResponse quoteRefund(String googleSub, UUID dueId) {
        MemberDue due = memberDueRepository.findByIdWithDetails(dueId)
                .orElseThrow(() -> new NotFoundException("회비 부과 기록을 찾을 수 없습니다."));
        requireAccess(googleSub, due);
        return quote(due);
    }

    @Transactional
    public DuesOverviewResponse recordRefund(String googleSub, UUID dueId, RecordDuesRefundRequest request) {
        Optional<DuesRefund> duplicate = refundRepository.findByIdempotencyKey(request.idempotencyKey());
        if (duplicate.isPresent()) {
            if (!duplicate.get().getMemberDue().getId().equals(dueId)) {
                throw new ConflictException("중복 방지 키가 다른 환불 기록에 사용되었습니다.");
            }
            return overviewForDue(googleSub, duplicate.get().getMemberDue());
        }
        MemberDue due = requireDueForUpdate(googleSub, dueId);
        if (due.getGenerationMember().getStatus() != GenerationMemberStatus.WITHDRAWN) {
            throw new ConflictException("탈퇴 처리된 부원만 환불을 기록할 수 있습니다.");
        }
        if (refundRepository.findFirstByMemberDueIdAndCanceledAtIsNull(dueId).isPresent()) {
            throw new ConflictException("이미 환불 처리가 기록되어 있습니다.");
        }
        DuesRefundQuoteResponse quote = quote(due);
        User user = userService.getByGoogleSub(googleSub);
        refundRepository.save(DuesRefund.create(due, new BigDecimal(quote.refundAmount()), quote.withdrawalDate(),
                quote.refundRateBps(), quote.ruleLabel(), request.idempotencyKey(), user));
        return overviewForDue(googleSub, due);
    }

    @Transactional
    public void createAssessmentIfPolicyExists(GenerationMember member) {
        policyRepository.findByGenerationId(member.getGeneration().getId())
                .ifPresent(policy -> createAssessment(policy, member, null));
    }

    public boolean hasPolicy(UUID generationId) { return policyRepository.existsByGenerationId(generationId); }

    private void createAssessment(GenerationDuesPolicy policy, GenerationMember member, User user) {
        if (memberDueRepository.existsByGenerationMemberId(member.getId())) return;
        boolean exempted = member.getDuesStatus() == GenerationMemberDuesStatus.EXEMPT;
        MemberDue due = memberDueRepository.save(MemberDue.create(policy, member, exempted,
                exempted ? "기존 면제 상태 이관" : null));
        if (member.getDuesStatus() == GenerationMemberDuesStatus.PAID) {
            if (user == null) throw new IllegalStateException("기존 납부 상태 이관에는 처리자가 필요합니다.");
            UUID key = UUID.nameUUIDFromBytes(("legacy-dues:" + policy.getId() + ":" + member.getId())
                    .getBytes(StandardCharsets.UTF_8));
            paymentRepository.save(DuesPayment.create(due, due.getAssessedAmount(), null,
                    DuesPaymentSource.LEGACY_STATUS, key, user));
        }
    }

    private List<RefundRuleRequest> validateRules(List<RefundRuleRequest> rules) {
        List<RefundRuleRequest> sorted = rules.stream().sorted(Comparator.comparing(RefundRuleRequest::endsOn)).toList();
        Set<LocalDate> dates = new HashSet<>();
        for (RefundRuleRequest rule : sorted) {
            if (!dates.add(rule.endsOn())) throw new InvalidRequestException("환불 기준일은 중복될 수 없습니다.");
        }
        return sorted;
    }

    private MemberDue requireDueForUpdate(String googleSub, UUID dueId) {
        MemberDue due = memberDueRepository.findByIdForUpdate(dueId)
                .orElseThrow(() -> new NotFoundException("회비 부과 기록을 찾을 수 없습니다."));
        requireAccess(googleSub, due);
        return due;
    }

    private void requireAccess(String googleSub, MemberDue due) {
        clubAccessService.requireAccessibleClub(googleSub,
                due.getPolicy().getGeneration().getClub().getId());
    }

    private DuesRefundQuoteResponse quote(MemberDue due) {
        DuesPayment payment = paymentRepository.findFirstByMemberDueIdAndCanceledAtIsNull(due.getId())
                .orElseThrow(() -> new ConflictException("납부가 확인되지 않아 환불을 계산할 수 없습니다."));
        LocalDate withdrawalDate = memberStatusHistoryRepository
                .findFirstByGenerationMemberIdAndNewStatusOrderByChangedAtDesc(
                        due.getGenerationMember().getId(), GenerationMemberStatus.WITHDRAWN)
                .orElseThrow(() -> new ConflictException("탈퇴 처리 이력이 없어 환불을 계산할 수 없습니다."))
                .getChangedAt().atZone(ZoneId.of("Asia/Seoul")).toLocalDate();
        GenerationDuesRefundRule rule = ruleRepository.findAllByPolicyIdOrderByEndsOnAsc(due.getPolicy().getId()).stream()
                .filter(candidate -> !withdrawalDate.isAfter(candidate.getEndsOn())).findFirst().orElse(null);
        int rate = rule == null ? 0 : rule.getRefundRateBps();
        String label = rule == null ? "환불 기간 종료" : rule.getLabel();
        BigDecimal amount = MoneyAmounts.refund(payment.getAmount(), rate);
        if (amount.compareTo(payment.getAmount()) > 0) throw new ConflictException("환불액이 납부액보다 큽니다.");
        return new DuesRefundQuoteResponse(MoneyAmounts.format(payment.getAmount()), MoneyAmounts.format(amount), rate, label, withdrawalDate);
    }

    private DuesOverviewResponse overviewForDue(String googleSub, MemberDue due) {
        UUID clubId = due.getPolicy().getGeneration().getClub().getId();
        return overview(googleSub, clubId, due.getPolicy().getGeneration().getId());
    }

    private DuesMemberRowResponse toRow(MemberDue due, DuesPayment payment, DuesRefund refund,
                                         BigDecimal paid, BigDecimal refunded) {
        String status;
        if (due.isExempted()) status = "EXEMPT";
        else if (refund != null && refunded.signum() == 0) status = "REFUND_NOT_APPLICABLE";
        else if (refund != null && refunded.compareTo(paid) == 0) status = "REFUNDED";
        else if (refund != null) status = "PARTIALLY_REFUNDED";
        else if (payment != null) status = "PAID";
        else status = "UNPAID";
        GenerationMember member = due.getGenerationMember();
        return new DuesMemberRowResponse(due.getId(), member.getId(), member.getPerson().getName(),
                member.getPerson().getStudentNumber(), member.getStatus(), MoneyAmounts.format(due.getAssessedAmount()),
                MoneyAmounts.format(paid), MoneyAmounts.format(refunded), status,
                payment == null ? null : payment.getPaidOn(), payment != null && payment.getSource() == DuesPaymentSource.LEGACY_STATUS);
    }
}
