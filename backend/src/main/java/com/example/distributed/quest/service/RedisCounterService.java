package com.example.distributed.quest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis计数器服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCounterService {

    private final StringRedisTemplate redisTemplate;

    private static final String COUNTER_PREFIX = "counter:questionnaire:";
    private static final String SUBMISSION_KEY = "submission:";

    /**
     * 增加计数器
     */
    public Long increment(String key, Duration ttl) {
        String fullKey = COUNTER_PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(fullKey);
        if (count != null && count == 1) {
            redisTemplate.expire(fullKey, ttl);
        }
        return count;
    }

    /**
     * 获取计数器值
     */
    public Long get(String key) {
        String fullKey = COUNTER_PREFIX + key;
        String value = redisTemplate.opsForValue().get(fullKey);
        return value != null ? Long.parseLong(value) : 0L;
    }

    /**
     * 记录用户提交次数
     */
    public Long incrementSubmissionCount(String userId, Long questionnaireId, Duration ttl) {
        String key = SUBMISSION_KEY + userId + ":" + questionnaireId;
        return increment(key, ttl);
    }

    /**
     * 获取用户提交次数
     */
    public Long getSubmissionCount(String userId, Long questionnaireId) {
        String key = SUBMISSION_KEY + userId + ":" + questionnaireId;
        return get(key);
    }

    /**
     * 删除计数器
     */
    public void delete(String key) {
        String fullKey = COUNTER_PREFIX + key;
        redisTemplate.delete(fullKey);
    }

    /**
     * 设置过期时间
     */
    public void setExpire(String key, Duration duration) {
        String fullKey = COUNTER_PREFIX + key;
        redisTemplate.expire(fullKey, duration);
    }
}
