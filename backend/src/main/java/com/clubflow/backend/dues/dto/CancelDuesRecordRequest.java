package com.clubflow.backend.dues.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CancelDuesRecordRequest(@NotBlank @Size(max = 500) String reason) {}
