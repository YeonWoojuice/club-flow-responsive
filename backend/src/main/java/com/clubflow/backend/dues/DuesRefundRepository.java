package com.clubflow.backend.dues;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface DuesRefundRepository extends JpaRepository<DuesRefund, UUID> {
    List<DuesRefund> findAllByMemberDuePolicyId(UUID policyId);
    Optional<DuesRefund> findByIdempotencyKey(UUID key);
    Optional<DuesRefund> findFirstByMemberDueIdAndCanceledAtIsNull(UUID memberDueId);
}
