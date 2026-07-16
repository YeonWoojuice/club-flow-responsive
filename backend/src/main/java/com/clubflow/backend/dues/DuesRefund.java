package com.clubflow.backend.dues;

import com.clubflow.backend.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "dues_refunds")
public class DuesRefund {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "member_due_id") private MemberDue memberDue;
    @Column(nullable = false, precision = 19, scale = 0) private BigDecimal amount;
    @Column(name = "withdrawal_date", nullable = false) private LocalDate withdrawalDate;
    @Column(name = "refund_rate_bps", nullable = false) private int refundRateBps;
    @Column(name = "rule_label_snapshot", nullable = false, length = 100) private String ruleLabelSnapshot;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "processed_by") private User processedBy;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;
    @Column(name = "canceled_at") private Instant canceledAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "canceled_by") private User canceledBy;
    @Column(name = "cancellation_reason", length = 500) private String cancellationReason;

    protected DuesRefund() {}
    private DuesRefund(MemberDue due, BigDecimal amount, LocalDate date, int rate, String label, UUID key, User user) {
        this.memberDue = due; this.amount = amount; this.withdrawalDate = date; this.refundRateBps = rate;
        this.ruleLabelSnapshot = label; this.idempotencyKey = key; this.processedBy = user; this.processedAt = Instant.now();
    }
    public static DuesRefund create(MemberDue due, BigDecimal amount, LocalDate date, int rate, String label, UUID key, User user) {
        return new DuesRefund(due, amount, date, rate, label, key, user);
    }
    public BigDecimal getAmount() { return amount; }
    public MemberDue getMemberDue() { return memberDue; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public Instant getCanceledAt() { return canceledAt; }
    public void cancel(User user, String reason) { this.canceledAt = Instant.now(); this.canceledBy = user; this.cancellationReason = reason; }
}
