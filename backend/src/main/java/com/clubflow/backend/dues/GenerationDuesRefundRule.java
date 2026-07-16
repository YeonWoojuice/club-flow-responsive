package com.clubflow.backend.dues;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "generation_dues_refund_rules")
public class GenerationDuesRefundRule {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "policy_id") private GenerationDuesPolicy policy;
    @Column(nullable = false, length = 100) private String label;
    @Column(name = "ends_on", nullable = false) private LocalDate endsOn;
    @Column(name = "refund_rate_bps", nullable = false) private int refundRateBps;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    protected GenerationDuesRefundRule() {}
    private GenerationDuesRefundRule(GenerationDuesPolicy policy, String label, LocalDate endsOn, int refundRateBps, int sortOrder) {
        this.policy = policy; this.label = label; this.endsOn = endsOn; this.refundRateBps = refundRateBps; this.sortOrder = sortOrder;
    }
    public static GenerationDuesRefundRule create(GenerationDuesPolicy policy, String label, LocalDate endsOn, int rate, int order) {
        return new GenerationDuesRefundRule(policy, label, endsOn, rate, order);
    }
    public String getLabel() { return label; }
    public LocalDate getEndsOn() { return endsOn; }
    public int getRefundRateBps() { return refundRateBps; }
}
