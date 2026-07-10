package com.clubflow.backend.member;

import com.clubflow.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "generation_member_status_histories")
public class GenerationMemberStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_member_id", nullable = false)
    private GenerationMember generationMember;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 20)
    private GenerationMemberStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private GenerationMemberStatus newStatus;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected GenerationMemberStatusHistory() {
    }

    private GenerationMemberStatusHistory(
            GenerationMember generationMember,
            GenerationMemberStatus previousStatus,
            GenerationMemberStatus newStatus,
            String reason,
            User changedBy
    ) {
        this.generationMember = generationMember;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedBy = changedBy;
        this.changedAt = Instant.now();
    }

    public static GenerationMemberStatusHistory create(
            GenerationMember generationMember,
            GenerationMemberStatus previousStatus,
            GenerationMemberStatus newStatus,
            String reason,
            User changedBy
    ) {
        return new GenerationMemberStatusHistory(
                generationMember, previousStatus, newStatus, reason, changedBy
        );
    }

    public UUID getId() { return id; }
    public GenerationMember getGenerationMember() { return generationMember; }
    public GenerationMemberStatus getPreviousStatus() { return previousStatus; }
    public GenerationMemberStatus getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public User getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
}
