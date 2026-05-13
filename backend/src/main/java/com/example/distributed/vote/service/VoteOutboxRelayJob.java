package com.example.distributed.vote.service;

import com.example.distributed.vote.config.VoteProperties;
import com.example.distributed.vote.domain.VoteOutboxStatus;
import com.example.distributed.vote.repository.VoteOutboxRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.vote", name = "outbox-relay-enabled", havingValue = "true", matchIfMissing = true)
public class VoteOutboxRelayJob {

    private final VoteOutboxRepository outboxRepository;
    private final VoteOutboxService outboxService;
    private final VoteProperties voteProperties;

    @Scheduled(fixedDelayString = "${app.vote.outbox-relay-delay-ms:3000}")
    public void relay() {
        List<com.example.distributed.vote.domain.VoteOutboxMessage> outboxes = outboxRepository.findRetryable(
                VoteOutboxStatus.SENT,
                Instant.now(),
                PageRequest.of(0, voteProperties.getOutboxBatchSize())
        );
        for (com.example.distributed.vote.domain.VoteOutboxMessage outbox : outboxes) {
            if (outbox.getRetryCount() >= voteProperties.getOutboxMaxRetry()) {
                continue;
            }
            outboxService.tryPublish(outbox);
        }
    }
}

