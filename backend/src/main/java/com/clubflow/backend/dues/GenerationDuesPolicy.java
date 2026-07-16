package com.clubflow.backend.dues;

import com.clubflow.backend.generation.Generation;
import com.clubflow.backend.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "generation_dues_policies")
public class GenerationDuesPolicy {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "generation_id") private Generation generation;
    @Column(nullable = false, precision = 19, scale = 0) private BigDecimal amount;
    @Column(name = "due_date") private LocalDate dueDate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by") private User createdBy;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected GenerationDuesPolicy() {}

    private GenerationDuesPolicy(Generation generation, BigDecimal amount, LocalDate dueDate, User createdBy) {
        this.generation = generation;
        this.amount = amount;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public static GenerationDuesPolicy create(Generation generation, BigDecimal amount, LocalDate dueDate, User createdBy) {
        return new GenerationDuesPolicy(generation, amount, dueDate, createdBy);
    }

    public UUID getId() { return id; }
    public Generation getGeneration() { return generation; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
}
