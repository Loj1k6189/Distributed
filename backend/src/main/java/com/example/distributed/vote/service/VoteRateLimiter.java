package com.example.distributed.vote.service;

import com.example.distributed.vote.config.VoteProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VoteRateLimiter {

    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT;

    static {
        SLIDING_WINDOW_SCRIPT = new DefaultRedisScript<>();
        SLIDING_WINDOW_SCRIPT.setScriptText("""
                redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1] - ARGV[2])
                local current = redis.call('ZCARD', KEYS[1])
                if current >= tonumber(ARGV[3]) then
                    return 0
                end
                redis.call('ZADD', KEYS[1], ARGV[1], ARGV[4])
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
                return 1
                """);
        SLIDING_WINDOW_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;
    private final VoteProperties voteProperties;

    public void validate(Long pollId, String voterId, String sourceIp, String requestId) {
        long now = System.currentTimeMillis();
        if (!allow(VoteRedisKeys.rateLimit(pollId, "user", voterId), now, requestId)) {
            throw new VoteBusinessException("RATE_LIMITED", "请求过快，请 1 秒后重试", HttpStatus.TOO_MANY_REQUESTS);
        }
        if (!allow(VoteRedisKeys.rateLimit(pollId, "ip", sourceIp), now, requestId)) {
            throw new VoteBusinessException("RATE_LIMITED", "当前 IP 请求过快，请 1 秒后重试", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    private boolean allow(String key, long now, String requestId) {
        Long allowed = redisTemplate.execute(
                SLIDING_WINDOW_SCRIPT,
                List.of(key),
                String.valueOf(now),
                String.valueOf(voteProperties.getRateLimitWindowMs()),
                String.valueOf(voteProperties.getRateLimitMaxRequests()),
                requestId
        );
        return Long.valueOf(1L).equals(allowed);
    }
}

