package com.example.distributed.vote.config;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.vote", name = "mq-enabled", havingValue = "true", matchIfMissing = true)
public class RedisStreamVoteConfig {

    private final StringRedisTemplate redisTemplate;
    private final VoteProperties voteProperties;

    @PostConstruct
    public void initializeConsumerGroup() {
        String streamKey = voteProperties.getMqStreamKey();
        RecordId seedId = redisTemplate.opsForStream()
                .add(StreamRecords.newRecord().in(streamKey).ofMap(Map.of("_seed", "1")));
        try {
            redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.latest(), voteProperties.getMqConsumerGroup());
        } catch (RuntimeException ex) {
            if (!isBusyGroup(ex)) {
                throw ex;
            }
        }
        if (seedId != null) {
            redisTemplate.opsForStream().delete(streamKey, seedId);
        }
    }

    private boolean isBusyGroup(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
