package com.clubflow.backend.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GenerationMemberStatusHistoryRepository
        extends JpaRepository<GenerationMemberStatusHistory, UUID> {

    @Query("""
            select history
            from GenerationMemberStatusHistory history
            join fetch history.changedBy changedBy
            where history.generationMember.id = :memberId
            order by history.changedAt desc, history.id desc
            """)
    List<GenerationMemberStatusHistory> findAllByMemberIdOrderByChangedAtDesc(
            @Param("memberId") UUID memberId
    );
}
