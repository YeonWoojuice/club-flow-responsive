package com.clubflow.backend.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {

    @Query("""
            select history
            from ApplicationStatusHistory history
            join fetch history.changedBy changedBy
            where history.application.id = :applicationId
            order by history.changedAt desc, history.id desc
            """)
    List<ApplicationStatusHistory> findAllByApplicationIdOrderByChangedAtDesc(
            @Param("applicationId") UUID applicationId
    );
}
