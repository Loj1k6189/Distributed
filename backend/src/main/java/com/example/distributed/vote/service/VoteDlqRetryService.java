package com.example.distributed.vote.service;

import com.example.distributed.vote.config.VoteProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.vote", name = "mq-enabled", havingValue = "true", matchIfMissing = true)
public class VoteDlqRetryService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final VoteProperties voteProperties;

    public int retryFromDlq(int maxRetry) {
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                StreamReadOptions.empty().count(maxRetry),
                StreamOffset.create(voteProperties.getMqDlqStreamKey(), ReadOffset.from("0-0"))
        );
        if (records == null || records.isEmpty()) {
            return 0;
        }

        int retried = 0;
        List<RecordId> successIds = new ArrayList<>();
        for (MapRecord<String, Object, Object> record : records) {
            VoteEventMessage message = toMessage(record);
            if (message == null) {
                successIds.add(record.getId());
                continue;
            }
            redisTemplate.opsForStream().add(StreamRecords.newRecord()
                    .in(voteProperties.getMqStreamKey())
                    .ofMap(java.util.Map.of(
                            "eventId", message.eventId(),
                            "payload", toPayload(message)
                    )));
            successIds.add(record.getId());
            retried++;
        }
        if (!successIds.isEmpty()) {
            redisTemplate.opsForStream().delete(
                    voteProperties.getMqDlqStreamKey(),
                    successIds.toArray(RecordId[]::new)
            );
        }
        return retried;
    }

    private VoteEventMessage toMessage(MapRecord<String, Object, Object> record) {
        Object payload = record.getValue().get("payload");
        if (!(payload instanceof String payloadText) || payloadText.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadText, VoteEventMessage.class);
        } catch (JacksonException ex) {
            throw new VoteBusinessException("DLQ_MESSAGE_FORMAT_ERROR", "死信消息格式不正确", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String toPayload(VoteEventMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JacksonException ex) {
            throw new VoteBusinessException("DLQ_MESSAGE_FORMAT_ERROR", "死信消息格式不正确", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
