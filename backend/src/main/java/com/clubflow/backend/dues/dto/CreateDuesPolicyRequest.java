package com.clubflow.backend.dues.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public record CreateDuesPolicyRequest(
        @NotBlank @Pattern(regexp = "[0-9]{1,19}", message = "회비는 1~19자리의 원 단위 숫자로 입력해 주세요.") String amount,
        LocalDate dueDate,
        @NotNull @Size(max = 20) List<@Valid RefundRuleRequest> refundRules
) {}
