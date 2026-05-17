package com.example.distributed.quest.service;

import com.example.distributed.quest.domain.QuestionEventMessage;
import com.example.distributed.quest.domain.QuestionOutboxMessage;
import com.example.distributed.quest.repository.QuestionOutboxRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionOutboxService {

    private final QuestionOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;

    public QuestionOutboxMessage saveOutbox(QuestionEventMessage eventMessage) {
        QuestionOutboxMessage outbox = new QuestionOutboxMessage();
        outbox.setEventId(eventMessage.getEventId());
        outbox.setPayload(toPayload(eventMessage));
        outbox.setStatus(com.example.distributed.quest.domain.QuestionOutboxStatus.NEW);
        outbox.setRetryCount(0);
        outbox.setCreatedAt(Instant.now());
        outbox.setUpdatedAt(Instant.now());
        return outboxRepository.save(outbox);
    }

    public boolean tryPublish(QuestionOutboxMessage outboxMessage) {
        try {
            var recordId = redisTemplate.opsForStream().add(org.springframework.data.redis.connection.stream.MapRecord.create("quest-stream", Map.of(
                    "eventId", outboxMessage.getEventId(),
                    "payload", outboxMessage.getPayload()
            )));
            
            if (recordId != null) {
                transactionTemplate.executeWithoutResult(status -> markSent(outboxMessage.getId()));
                return true;
            }
            return false;
        } catch (RuntimeException ex) {
            transactionTemplate.executeWithoutResult(status -> markFailed(outboxMessage.getId()));
            return false;
        }
    }

    public void markSent(Long outboxId) {
        QuestionOutboxMessage outboxMessage = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new QuestionBusinessException("OUTBOX_NOT_FOUND", "事务消息不存在"));
        outboxMessage.setStatus(com.example.distributed.quest.domain.QuestionOutboxStatus.SENT);
        outboxMessage.setUpdatedAt(Instant.now());
        outboxMessage.setNextRetryAt(null);
        outboxRepository.save(outboxMessage);
    }

    public void markFailed(Long outboxId) {
        QuestionOutboxMessage outboxMessage = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new QuestionBusinessException("OUTBOX_NOT_FOUND", "事务消息不存在"));
        int retries = outboxMessage.getRetryCount() + 1;
        outboxMessage.setRetryCount(retries);
        outboxMessage.setStatus(com.example.distributed.quest.domain.QuestionOutboxStatus.FAILED);
        outboxMessage.setUpdatedAt(Instant.now());
        long delayMultiplier = 1L << Math.min(retries - 1, 8); // 最大指数退避限制
        long delayMs = 1000L * delayMultiplier; // 基础延迟1秒
        outboxMessage.setNextRetryAt(Instant.now().plusMillis(delayMs));
        outboxRepository.save(outboxMessage);
    }

    private String toPayload(QuestionEventMessage eventMessage) {
        try {
            return objectMapper.writeValueAsString(eventMessage);
        } catch (JacksonException ex) {
            throw new QuestionBusinessException("OUTBOX_SERIALIZE_ERROR", "事务消息序列化失败");
        }
    }

    private QuestionEventMessage fromPayload(String payload) {
        try {
            return objectMapper.readValue(payload, QuestionEventMessage.class);
        } catch (JacksonException ex) {
            throw new QuestionBusinessException("OUTBOX_DESERIALIZE_ERROR", "事务消息反序列化失败");
        }
    }
}