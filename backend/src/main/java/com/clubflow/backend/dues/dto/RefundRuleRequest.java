package com.clubflow.backend.dues.dto;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record RefundRuleRequest(@NotBlank @Size(max = 100) String label, @NotNull LocalDate endsOn,
                                @Min(0) @Max(10000) int refundRateBps) {}
