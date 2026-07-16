package com.clubflow.backend.dues;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface DuesPaymentRepository extends JpaRepository<DuesPayment, UUID> {
    List<DuesPayment> findAllByMemberDuePolicyId(UUID policyId);
    Optional<DuesPayment> findByIdempotencyKey(UUID key);
    Optional<DuesPayment> findFirstByMemberDueIdAndCanceledAtIsNull(UUID memberDueId);
}
