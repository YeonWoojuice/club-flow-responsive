package com.clubflow.backend.dues.dto;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public record DuesOverviewResponse(UUID policyId, String amount, LocalDate dueDate,
        List<DuesRefundRuleResponse> refundRules, String totalAssessed, String totalPaid,
        String totalRefunded, int unpaidCount, List<DuesMemberRowResponse> members) {}
