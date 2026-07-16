package com.clubflow.backend.dues;

import com.clubflow.backend.member.GenerationMember;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "member_dues")
public class MemberDue {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "policy_id") private GenerationDuesPolicy policy;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "generation_member_id") private GenerationMember generationMember;
    @Column(name = "assessed_amount", nullable = false, precision = 19, scale = 0) private BigDecimal assessedAmount;
    @Column(nullable = false) private boolean exempted;
    @Column(name = "exemption_reason", length = 500) private String exemptionReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected MemberDue() {}
    private MemberDue(GenerationDuesPolicy policy, GenerationMember member, boolean exempted, String reason) {
        this.policy = policy; this.generationMember = member; this.assessedAmount = policy.getAmount();
        this.exempted = exempted; this.exemptionReason = reason; this.createdAt = Instant.now();
    }
    public static MemberDue create(GenerationDuesPolicy policy, GenerationMember member, boolean exempted, String reason) {
        return new MemberDue(policy, member, exempted, reason);
    }
    public UUID getId() { return id; }
    public GenerationDuesPolicy getPolicy() { return policy; }
    public GenerationMember getGenerationMember() { return generationMember; }
    public BigDecimal getAssessedAmount() { return assessedAmount; }
    public boolean isExempted() { return exempted; }
    public String getExemptionReason() { return exemptionReason; }
    public void changeExemption(boolean exempted, String reason) { this.exempted = exempted; this.exemptionReason = reason; }
}
