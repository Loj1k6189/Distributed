package com.example.distributed.vote.service;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.vote", name = {"mq-enabled", "consumer-enabled"}, havingValue = "true", matchIfMissing = true)
public class VoteEventConsumer {

    private final VoteIdempotencyService idempotencyService;
    private final VotePersistenceService persistenceService;

    @RabbitListener(queues = "${app.vote.mq-queue}", containerFactory = "voteBatchListenerContainerFactory")
    public void onBatchMessage(List<VoteEventMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        List<VoteEventMessage> accepted = new ArrayList<>();
        List<String> reserved = new ArrayList<>();
        for (VoteEventMessage message : messages) {
            if (idempotencyService.reserve(message.eventId())) {
                accepted.add(message);
                reserved.add(message.eventId());
            }
        }
        if (accepted.isEmpty()) {
            return;
        }
        try {
            persistenceService.persistBatch(accepted);
        } catch (RuntimeException ex) {
            reserved.forEach(idempotencyService::release);
            throw ex;
        }
    }
}

