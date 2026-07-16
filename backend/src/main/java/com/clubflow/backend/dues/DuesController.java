package com.clubflow.backend.dues;

import com.clubflow.backend.dues.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DuesController {
    private final DuesService duesService;
    public DuesController(DuesService duesService) { this.duesService = duesService; }

    @GetMapping("/clubs/{clubId}/generations/{generationId}/dues")
    public DuesOverviewResponse overview(@AuthenticationPrincipal OidcUser user, @PathVariable UUID clubId,
                                         @PathVariable UUID generationId) {
        return duesService.overview(user.getSubject(), clubId, generationId);
    }

    @PostMapping("/clubs/{clubId}/generations/{generationId}/dues/policy")
    @ResponseStatus(HttpStatus.CREATED)
    public DuesOverviewResponse createPolicy(@AuthenticationPrincipal OidcUser user, @PathVariable UUID clubId,
                                              @PathVariable UUID generationId,
                                              @Valid @RequestBody CreateDuesPolicyRequest request) {
        return duesService.createPolicy(user.getSubject(), clubId, generationId, request);
    }

    @PostMapping("/member-dues/{dueId}/payments")
    public DuesOverviewResponse recordPayment(@AuthenticationPrincipal OidcUser user, @PathVariable UUID dueId,
                                               @Valid @RequestBody RecordDuesPaymentRequest request) {
        return duesService.recordPayment(user.getSubject(), dueId, request);
    }

    @PatchMapping("/member-dues/{dueId}/exemption")
    public DuesOverviewResponse changeExemption(@AuthenticationPrincipal OidcUser user, @PathVariable UUID dueId,
                                                 @Valid @RequestBody ChangeDuesExemptionRequest request) {
        return duesService.changeExemption(user.getSubject(), dueId, request);
    }

    @PostMapping("/member-dues/{dueId}/payments/cancel")
    public DuesOverviewResponse cancelPayment(@AuthenticationPrincipal OidcUser user, @PathVariable UUID dueId,
                                               @Valid @RequestBody CancelDuesRecordRequest request) {
        return duesService.cancelPayment(user.getSubject(), dueId, request);
    }

    @PostMapping("/member-dues/{dueId}/refunds/cancel")
    public DuesOverviewResponse cancelRefund(@AuthenticationPrincipal OidcUser user, @PathVariable UUID dueId,
                                              @Valid @RequestBody CancelDuesRecordRequest request) {
        return duesService.cancelRefund(user.getSubject(), dueId, request);
    }

    @GetMapping("/member-dues/{dueId}/refund-quote")
    public DuesRefundQuoteResponse refundQuote(@AuthenticationPrincipal OidcUser user, @PathVariable UUID dueId) {
        return duesService.quoteRefund(user.getSubject(), dueId);
    }

    @PostMapping("/member-dues/{dueId}/refunds")
    public DuesOverviewResponse recordRefund(@AuthenticationPrincipal OidcUser user, @PathVariable UUID dueId,
                                              @Valid @RequestBody RecordDuesRefundRequest request) {
        return duesService.recordRefund(user.getSubject(), dueId, request);
    }
}
