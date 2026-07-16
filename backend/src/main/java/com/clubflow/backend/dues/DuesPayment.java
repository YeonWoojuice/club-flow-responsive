package com.clubflow.backend.dues;

import com.clubflow.backend.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "dues_payments")
public class DuesPayment {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "member_due_id") private MemberDue memberDue;
    @Column(nullable = false, precision = 19, scale = 0) private BigDecimal amount;
    @Column(name = "paid_on") private LocalDate paidOn;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private DuesPaymentSource source;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recorded_by") private User recordedBy;
    @Column(name = "recorded_at", nullable = false) private Instant recordedAt;
    @Column(name = "canceled_at") private Instant canceledAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "canceled_by") private User canceledBy;
    @Column(name = "cancellation_reason", length = 500) private String cancellationReason;

    protected DuesPayment() {}
    private DuesPayment(MemberDue due, BigDecimal amount, LocalDate paidOn, DuesPaymentSource source, UUID key, User user) {
        this.memberDue = due; this.amount = amount; this.paidOn = paidOn; this.source = source;
        this.idempotencyKey = key; this.recordedBy = user; this.recordedAt = Instant.now();
    }
    public static DuesPayment create(MemberDue due, BigDecimal amount, LocalDate paidOn, DuesPaymentSource source, UUID key, User user) {
        return new DuesPayment(due, amount, paidOn, source, key, user);
    }
    public BigDecimal getAmount() { return amount; }
    public MemberDue getMemberDue() { return memberDue; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public LocalDate getPaidOn() { return paidOn; }
    public DuesPaymentSource getSource() { return source; }
    public Instant getCanceledAt() { return canceledAt; }
    public void cancel(User user, String reason) { this.canceledAt = Instant.now(); this.canceledBy = user; this.cancellationReason = reason; }
}
