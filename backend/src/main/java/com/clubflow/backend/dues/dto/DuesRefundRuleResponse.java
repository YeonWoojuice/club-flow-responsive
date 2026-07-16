package com.clubflow.backend.dues.dto;
import java.time.LocalDate;
public record DuesRefundRuleResponse(String label, LocalDate endsOn, int refundRateBps) {}
