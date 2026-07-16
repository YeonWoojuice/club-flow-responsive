package com.clubflow.backend.dues.dto;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
public record RecordDuesPaymentRequest(@NotNull LocalDate paidOn, @NotNull UUID idempotencyKey) {}
