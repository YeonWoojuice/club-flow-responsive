package com.clubflow.backend.dues;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface MemberDueRepository extends JpaRepository<MemberDue, UUID> {
    boolean existsByGenerationMemberId(UUID memberId);
    @Query("""
        select due from MemberDue due join fetch due.generationMember member join fetch member.person person
        where due.policy.id = :policyId order by person.name asc
        """)
    List<MemberDue> findAllForOverview(@Param("policyId") UUID policyId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select due from MemberDue due join fetch due.policy policy join fetch policy.generation generation
        join fetch generation.club club join fetch due.generationMember member join fetch member.person person
        where due.id = :id
        """)
    Optional<MemberDue> findByIdForUpdate(@Param("id") UUID id);
    @Query("""
        select due from MemberDue due join fetch due.policy policy join fetch policy.generation generation
        join fetch generation.club club join fetch due.generationMember member join fetch member.person person
        where due.id = :id
        """)
    Optional<MemberDue> findByIdWithDetails(@Param("id") UUID id);
}
