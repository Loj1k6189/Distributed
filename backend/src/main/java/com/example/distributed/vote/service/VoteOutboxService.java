package com.example.distributed.vote.service;

import com.example.distributed.vote.config.VoteProperties;
import com.example.distributed.vote.domain.VoteOutboxMessage;
import com.example.distributed.vote.domain.VoteOutboxStatus;
import com.example.distributed.vote.repository.VoteOutboxRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class VoteOutboxService {

    private final VoteOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;
    private final VoteProperties voteProperties;
    private final TransactionTemplate transactionTemplate;

    @Transactional
    public VoteOutboxMessage saveOutbox(VoteEventMessage eventMessage) {
        VoteOutboxMessage outbox = new VoteOutboxMessage();
        outbox.setEventId(eventMessage.eventId());
        outbox.setPayload(toPayload(eventMessage));
        outbox.setStatus(VoteOutboxStatus.NEW);
        outbox.setRetryCount(0);
        outbox.setCreatedAt(Instant.now());
        outbox.setUpdatedAt(Instant.now());
        return outboxRepository.save(outbox);
    }

    public boolean tryPublish(VoteOutboxMessage outboxMessage) {
        RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
        if (rabbitTemplate == null) {
            return false;
        }
        VoteEventMessage message = fromPayload(outboxMessage.getPayload());
        try {
            rabbitTemplate.convertAndSend(
                    voteProperties.getMqExchange(),
                    voteProperties.getMqRoutingKey(),
                    message,
                    msg -> {
                        msg.getMessageProperties().setMessageId(message.eventId());
                        msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return msg;
                    }
            );
            transactionTemplate.executeWithoutResult(status -> markSent(outboxMessage.getId()));
            return true;
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> markFailed(outboxMessage.getId()));
            return false;
        }
    }

    @Transactional
    public void markSent(Long outboxId) {
        VoteOutboxMessage outboxMessage = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new VoteBusinessException("OUTBOX_NOT_FOUND", "事务消息不存在", HttpStatus.NOT_FOUND));
        outboxMessage.setStatus(VoteOutboxStatus.SENT);
        outboxMessage.setUpdatedAt(Instant.now());
        outboxMessage.setNextRetryAt(null);
        outboxRepository.save(outboxMessage);
    }

    @Transactional
    public void markFailed(Long outboxId) {
        VoteOutboxMessage outboxMessage = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new VoteBusinessException("OUTBOX_NOT_FOUND", "事务消息不存在", HttpStatus.NOT_FOUND));
        int retries = outboxMessage.getRetryCount() + 1;
        outboxMessage.setRetryCount(retries);
        outboxMessage.setStatus(VoteOutboxStatus.FAILED);
        outboxMessage.setUpdatedAt(Instant.now());
        long exponent = Math.max(0, retries - 1);
        long delayMultiplier = 1L << Math.min(exponent, 8);
        long delayMs = voteProperties.getOutboxRetryBaseDelay().toMillis() * delayMultiplier;
        outboxMessage.setNextRetryAt(Instant.now().plusMillis(delayMs));
        outboxRepository.save(outboxMessage);
    }

    private String toPayload(VoteEventMessage eventMessage) {
        try {
            return objectMapper.writeValueAsString(eventMessage);
        } catch (JacksonException ex) {
            throw new VoteBusinessException("OUTBOX_SERIALIZE_ERROR", "事务消息序列化失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private VoteEventMessage fromPayload(String payload) {
        try {
            return objectMapper.readValue(payload, VoteEventMessage.class);
        } catch (JacksonException ex) {
            throw new VoteBusinessException("OUTBOX_DESERIALIZE_ERROR", "事务消息反序列化失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
