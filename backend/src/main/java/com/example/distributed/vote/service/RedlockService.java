package com.example.distributed.vote.service;

import com.example.distributed.vote.config.VoteProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedlockService {

    private static final byte[] RELEASE_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """.getBytes(StandardCharsets.UTF_8);

    private final StringRedisTemplate redisTemplate;
    private final VoteProperties voteProperties;

    public RedlockToken acquire(String resource) {
        int replicas = Math.max(3, voteProperties.getRedlockReplicas());
        int quorum = replicas / 2 + 1;
        String token = UUID.randomUUID().toString();
        long start = System.nanoTime();
        Duration lease = Duration.ofMillis(voteProperties.getLockLeaseMs());
        List<String> lockedKeys = new ArrayList<>(replicas);
        for (int i = 0; i < replicas; i++) {
            String lockKey = VoteRedisKeys.redlock(resource, i);
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, token, lease);
            if (Boolean.TRUE.equals(locked)) {
                lockedKeys.add(lockKey);
            }
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        if (lockedKeys.size() >= quorum && elapsedMs < lease.toMillis()) {
            return new RedlockToken(token, lockedKeys);
        }
        unlock(new RedlockToken(token, lockedKeys));
        throw new VoteBusinessException("REDLOCK_ACQUIRE_FAILED", "系统繁忙，请稍后重试", HttpStatus.CONFLICT);
    }

    public void unlock(RedlockToken redlockToken) {
        for (String lockKey : redlockToken.lockKeys()) {
            try {
                redisTemplate.execute(connection -> connection.scriptingCommands()
                        .eval(RELEASE_SCRIPT, ReturnType.INTEGER, 1,
                                lockKey.getBytes(StandardCharsets.UTF_8),
                                redlockToken.token().getBytes(StandardCharsets.UTF_8)), true);
            } catch (DataAccessException ignored) {
                // lock lease will eventually expire
            }
        }
    }

    public record RedlockToken(String token, List<String> lockKeys) {
    }
}

