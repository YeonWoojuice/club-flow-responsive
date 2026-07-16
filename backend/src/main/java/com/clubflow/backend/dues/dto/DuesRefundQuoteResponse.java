package com.clubflow.backend.dues.dto;
import java.time.LocalDate;
public record DuesRefundQuoteResponse(String paidAmount, String refundAmount, int refundRateBps,
                                      String ruleLabel, LocalDate withdrawalDate) {}
