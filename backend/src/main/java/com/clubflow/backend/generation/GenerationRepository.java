package com.clubflow.backend.generation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GenerationRepository extends JpaRepository<Generation, UUID> {

    List<Generation> findAllByClubIdOrderByCreatedAtDesc(UUID clubId);

    Optional<Generation> findByIdAndClubId(UUID id, UUID clubId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Generation> findForUpdateByIdAndClubId(UUID id, UUID clubId);

    boolean existsByClubIdAndStatus(UUID clubId, GenerationStatus status);
}
