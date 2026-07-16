package com.clubflow.backend.dues.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record ChangeDuesExemptionRequest(@NotNull Boolean exempted, @Size(max = 500) String reason) {}
