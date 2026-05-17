package com.example.distributed.vote.service;

import com.example.distributed.vote.config.VoteProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.vote", name = {"mq-enabled", "consumer-enabled"}, havingValue = "true", matchIfMissing = true)
public class VoteEventConsumer {

    private final VoteProperties voteProperties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final VoteIdempotencyService idempotencyService;
    private final VotePersistenceService persistenceService;

    @Scheduled(fixedDelayString = "${app.vote.consumer-poll-delay-ms:500}")
    public void consumeBatch() {
        List<MapRecord<String, Object, Object>> records = readPendingFirst();
        if (records == null || records.isEmpty()) {
            return;
        }

        List<VoteEventMessage> accepted = new ArrayList<>();
        List<String> reserved = new ArrayList<>();
        List<RecordId> nonTransactionalAckIds = new ArrayList<>();
        List<RecordId> transactionalAckIds = new ArrayList<>();

        for (MapRecord<String, Object, Object> record : records) {
            VoteEventMessage message = tryDecode(record);
            if (message == null) {
                moveToDlq(record, "INVALID_PAYLOAD");
                nonTransactionalAckIds.add(record.getId());
                continue;
            }
            if (idempotencyService.reserve(message.eventId())) {
                accepted.add(message);
                reserved.add(message.eventId());
                transactionalAckIds.add(record.getId());
            } else {
                nonTransactionalAckIds.add(record.getId());
            }
        }

        acknowledge(nonTransactionalAckIds);
        if (accepted.isEmpty()) {
            return;
        }
        try {
            persistenceService.persistBatch(accepted);
        } catch (RuntimeException ex) {
            reserved.forEach(idempotencyService::release);
            throw ex;
        }
        acknowledge(transactionalAckIds);
    }

    private List<MapRecord<String, Object, Object>> readPendingFirst() {
        List<MapRecord<String, Object, Object>> pending = readBatch(ReadOffset.from("0"));
        if (pending != null && !pending.isEmpty()) {
            return pending;
        }
        return readBatch(ReadOffset.lastConsumed());
    }

    private List<MapRecord<String, Object, Object>> readBatch(ReadOffset readOffset) {
        return redisTemplate.opsForStream().read(
                Consumer.from(voteProperties.getMqConsumerGroup(), voteProperties.getMqConsumerName()),
                StreamReadOptions.empty().count(voteProperties.getMqBatchSize()),
                StreamOffset.create(voteProperties.getMqStreamKey(), readOffset)
        );
    }

    private VoteEventMessage tryDecode(MapRecord<String, Object, Object> record) {
        Object payload = record.getValue().get("payload");
        if (!(payload instanceof String payloadText) || payloadText.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadText, VoteEventMessage.class);
        } catch (JacksonException ex) {
            return null;
        }
    }

    private void moveToDlq(MapRecord<String, Object, Object> record, String reason) {
        redisTemplate.opsForStream().add(StreamRecords.newRecord()
                .in(voteProperties.getMqDlqStreamKey())
                .ofMap(java.util.Map.of(
                        "sourceRecordId", record.getId().getValue(),
                        "payload", String.valueOf(record.getValue().get("payload")),
                        "reason", reason,
                        "failedAt", Instant.now().toString()
                )));
    }

    private void acknowledge(List<RecordId> ids) {
        if (ids.isEmpty()) {
            return;
        }
        RecordId[] recordIds = ids.toArray(RecordId[]::new);
        redisTemplate.opsForStream().acknowledge(
                voteProperties.getMqStreamKey(),
                voteProperties.getMqConsumerGroup(),
                recordIds
        );
    }
}
