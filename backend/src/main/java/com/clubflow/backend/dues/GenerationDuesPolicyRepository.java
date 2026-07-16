package com.clubflow.backend.dues;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface GenerationDuesPolicyRepository extends JpaRepository<GenerationDuesPolicy, UUID> {
    Optional<GenerationDuesPolicy> findByGenerationId(UUID generationId);
    boolean existsByGenerationId(UUID generationId);
}
