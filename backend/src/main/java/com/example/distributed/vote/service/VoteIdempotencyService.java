package com.example.distributed.vote.service;

import com.example.distributed.vote.config.VoteProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoteIdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final VoteProperties voteProperties;

    public boolean reserve(String eventId) {
        Duration ttl = voteProperties.getIdempotentTtl();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(VoteRedisKeys.idempotent(eventId), "1", ttl);
        return Boolean.TRUE.equals(locked);
    }

    public void release(String eventId) {
        redisTemplate.delete(VoteRedisKeys.idempotent(eventId));
    }
}

