package com.clubflow.backend.application.email;

import com.clubflow.backend.application.ApplicationStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ApplicationResultEmailQueryService {

    private final ApplicationResultEmailMessageRepository messageRepository;

    public ApplicationResultEmailQueryService(ApplicationResultEmailMessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public Map<UUID, ResultEmailState> latestStates(Map<UUID, ApplicationStatus> decisionsByApplicationId) {
        if (decisionsByApplicationId.isEmpty()) return Map.of();
        Map<UUID, ResultEmailState> states = new HashMap<>();
        messageRepository.findAllByApplication_IdInOrderByCreatedAtDesc(decisionsByApplicationId.keySet())
                .stream()
                .filter(message -> message.getDecision() == decisionsByApplicationId.get(message.getApplicationId()))
                .forEach(message -> states.putIfAbsent(
                    message.getApplicationId(),
                    new ResultEmailState(
                            ApplicationResultEmailStatus.from(message.getStatus()),
                            message.getSentAt()
                    )
            ));
        return states;
    }

    public Map<UUID, ResultEmailState> latestStates(Set<UUID> applicationIds, ApplicationStatus decision) {
        return latestStates(applicationIds.stream().collect(java.util.stream.Collectors.toMap(
                applicationId -> applicationId,
                applicationId -> decision
        )));
    }

    public record ResultEmailState(ApplicationResultEmailStatus status, Instant sentAt) {
        public static ResultEmailState notSent() {
            return new ResultEmailState(ApplicationResultEmailStatus.NOT_SENT, null);
        }
    }
}
