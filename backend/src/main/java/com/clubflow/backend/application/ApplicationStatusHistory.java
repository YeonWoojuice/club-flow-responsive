package com.clubflow.backend.application;

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
@Table(name = "application_status_histories")
public class ApplicationStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 20)
    private ApplicationStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private ApplicationStatus newStatus;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ApplicationStatusHistory() {
    }

    private ApplicationStatusHistory(
            Application application,
            ApplicationStatus previousStatus,
            ApplicationStatus newStatus,
            String reason,
            User changedBy
    ) {
        this.application = application;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.changedBy = changedBy;
        this.changedAt = Instant.now();
    }

    public static ApplicationStatusHistory create(
            Application application,
            ApplicationStatus previousStatus,
            ApplicationStatus newStatus,
            String reason,
            User changedBy
    ) {
        return new ApplicationStatusHistory(application, previousStatus, newStatus, reason, changedBy);
    }

    public UUID getId() { return id; }
    public ApplicationStatus getPreviousStatus() { return previousStatus; }
    public ApplicationStatus getNewStatus() { return newStatus; }
    public String getReason() { return reason; }
    public User getChangedBy() { return changedBy; }
    public Instant getChangedAt() { return changedAt; }
}
