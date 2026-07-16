package com.clubflow.backend.dues;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface GenerationDuesRefundRuleRepository extends JpaRepository<GenerationDuesRefundRule, UUID> {
    List<GenerationDuesRefundRule> findAllByPolicyIdOrderByEndsOnAsc(UUID policyId);
}
