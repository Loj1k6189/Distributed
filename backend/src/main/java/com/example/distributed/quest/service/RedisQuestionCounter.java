package com.example.distributed.quest.service;

import com.example.distributed.quest.service.QuestionBusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisQuestionCounter {

    private static final String BALLOTS_FIELD = "__ballots";
    private static final DefaultRedisScript<Long> QUESTION_INCREMENT_SCRIPT;
    private static final DefaultRedisScript<Long> QUESTION_DECREMENT_SCRIPT;

    static {
        QUESTION_INCREMENT_SCRIPT = new DefaultRedisScript<>();
        QUESTION_INCREMENT_SCRIPT.setScriptText("""
                for i = 1, #ARGV do
                    if redis.call('SISMEMBER', KEYS[1], ARGV[i]) == 0 then
                        return -1
                    end
                end
                for i = 1, #ARGV do
                    redis.call('HINCRBY', KEYS[2], ARGV[i], 1)
                end
                redis.call('HINCRBY', KEYS[2], '__ballots', 1)
                return 1
                """);
        QUESTION_INCREMENT_SCRIPT.setResultType(Long.class);

        QUESTION_DECREMENT_SCRIPT = new DefaultRedisScript<>();
        QUESTION_DECREMENT_SCRIPT.setScriptText("""
                for i = 1, #ARGV do
                    if redis.call('HEXISTS', KEYS[1], ARGV[i]) == 1 then
                        redis.call('HINCRBY', KEYS[1], ARGV[i], -1)
                    end
                end
                if redis.call('HEXISTS', KEYS[1], '__ballots') == 1 then
                    redis.call('HINCRBY', KEYS[1], '__ballots', -1)
                end
                return 1
                """);
        QUESTION_DECREMENT_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    public void initializeQuestion(Long questionId, List<Long> optionIds) {
        String optionKey = QuestRedisKeys.questionOptions(questionId);
        String countKey = QuestRedisKeys.questionCounts(questionId);
        redisTemplate.delete(List.of(optionKey, countKey));
        List<String> optionMembers = optionIds.stream().map(String::valueOf).toList();
        redisTemplate.opsForSet().add(optionKey, optionMembers.toArray(String[]::new));
        Map<String, String> initialCount = new LinkedHashMap<>();
        for (Long optionId : optionIds) {
            initialCount.put(String.valueOf(optionId), "0");
        }
        initialCount.put(BALLOTS_FIELD, "0");
        redisTemplate.opsForHash().putAll(countKey, initialCount);
    }

    public void incrementQuestion(Long questionId, List<Long> optionIds) {
        List<String> args = optionIds.stream().map(String::valueOf).toList();
        Object[] scriptArgs = args.toArray();
        Long result = redisTemplate.execute(
                QUESTION_INCREMENT_SCRIPT,
                List.of(QuestRedisKeys.questionOptions(questionId), QuestRedisKeys.questionCounts(questionId)),
                scriptArgs
        );
        if (Long.valueOf(-1L).equals(result)) {
            throw new QuestionBusinessException("INVALID_OPTION", "问卷选项无效");
        }
        if (!Long.valueOf(1L).equals(result)) {
            throw new QuestionBusinessException("REDIS_UPDATE_FAILED", "问卷计数失败");
        }
    }

    public void rollbackIncrement(Long questionId, List<Long> optionIds) {
        List<String> args = optionIds.stream().map(String::valueOf).toList();
        Object[] scriptArgs = args.toArray();
        redisTemplate.execute(
                QUESTION_DECREMENT_SCRIPT,
                List.of(QuestRedisKeys.questionCounts(questionId)),
                scriptArgs
        );
    }

    public Map<Long, Long> pollOptionCounts(Long questionId, List<Long> optionIds) {
        List<Object> hashKeys = optionIds.stream()
                .map(String::valueOf)
                .map(key -> (Object) key)
                .toList();
        List<Object> values = redisTemplate.opsForHash()
                .multiGet(QuestRedisKeys.questionCounts(questionId), hashKeys);
        Map<Long, Long> result = new LinkedHashMap<>();
        for (int i = 0; i < optionIds.size(); i++) {
            Object value = values == null ? null : values.get(i);
            result.put(optionIds.get(i), toLong(value));
        }
        return result;
    }

    public long pollBallots(Long questionId) {
        Object ballots = redisTemplate.opsForHash().get(QuestRedisKeys.questionCounts(questionId), BALLOTS_FIELD);
        return toLong(ballots);
    }

    public void replayQuestion(Long questionId, List<Long> optionIds) {
        for (Long optionId : optionIds) {
            redisTemplate.opsForHash().increment(QuestRedisKeys.questionCounts(questionId), String.valueOf(optionId), 1L);
        }
        redisTemplate.opsForHash().increment(QuestRedisKeys.questionCounts(questionId), BALLOTS_FIELD, 1L);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}