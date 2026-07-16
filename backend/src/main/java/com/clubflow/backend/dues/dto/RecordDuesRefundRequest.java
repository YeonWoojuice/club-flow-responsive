package com.clubflow.backend.dues.dto;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record RecordDuesRefundRequest(@NotNull UUID idempotencyKey) {}
