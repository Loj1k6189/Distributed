package com.example.distributed.vote.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisVoteCounter {

    private static final String BALLOTS_FIELD = "__ballots";
    private static final DefaultRedisScript<Long> VOTE_INCREMENT_SCRIPT;
    private static final DefaultRedisScript<Long> VOTE_DECREMENT_SCRIPT;

    static {
        VOTE_INCREMENT_SCRIPT = new DefaultRedisScript<>();
        VOTE_INCREMENT_SCRIPT.setScriptText("""
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
        VOTE_INCREMENT_SCRIPT.setResultType(Long.class);

        VOTE_DECREMENT_SCRIPT = new DefaultRedisScript<>();
        VOTE_DECREMENT_SCRIPT.setScriptText("""
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
        VOTE_DECREMENT_SCRIPT.setResultType(Long.class);
    }

    private final StringRedisTemplate redisTemplate;

    public void initializePoll(Long pollId, List<Long> optionIds) {
        String optionKey = VoteRedisKeys.pollOptions(pollId);
        String countKey = VoteRedisKeys.pollCounts(pollId);
        String votersKey = VoteRedisKeys.pollVoters(pollId);
        redisTemplate.delete(List.of(optionKey, countKey, votersKey));
        List<String> optionMembers = optionIds.stream().map(String::valueOf).toList();
        redisTemplate.opsForSet().add(optionKey, optionMembers.toArray(String[]::new));
        Map<String, String> initialCount = new LinkedHashMap<>();
        for (Long optionId : optionIds) {
            initialCount.put(String.valueOf(optionId), "0");
        }
        initialCount.put(BALLOTS_FIELD, "0");
        redisTemplate.opsForHash().putAll(countKey, initialCount);
    }

    public void setPollCounts(Long pollId, List<Long> optionIds, Map<Long, Long> counts, long ballots) {
        String optionKey = VoteRedisKeys.pollOptions(pollId);
        String countKey = VoteRedisKeys.pollCounts(pollId);
        String votersKey = VoteRedisKeys.pollVoters(pollId);
        redisTemplate.delete(List.of(optionKey, countKey, votersKey));
        List<String> optionMembers = optionIds.stream().map(String::valueOf).toList();
        redisTemplate.opsForSet().add(optionKey, optionMembers.toArray(String[]::new));
        Map<String, String> counter = new LinkedHashMap<>();
        for (Long optionId : optionIds) {
            counter.put(String.valueOf(optionId), String.valueOf(counts.getOrDefault(optionId, 0L)));
        }
        counter.put(BALLOTS_FIELD, String.valueOf(Math.max(ballots, 0L)));
        redisTemplate.opsForHash().putAll(countKey, counter);
    }

    public void incrementVote(Long pollId, List<Long> optionIds) {
        List<String> args = optionIds.stream().map(String::valueOf).toList();
        Object[] scriptArgs = args.toArray();
        Long result = redisTemplate.execute(
                VOTE_INCREMENT_SCRIPT,
                List.of(VoteRedisKeys.pollOptions(pollId), VoteRedisKeys.pollCounts(pollId)),
                scriptArgs
        );
        if (Long.valueOf(-1L).equals(result)) {
            throw new VoteBusinessException("INVALID_OPTION", "投票选项无效", HttpStatus.BAD_REQUEST);
        }
        if (!Long.valueOf(1L).equals(result)) {
            throw new VoteBusinessException("REDIS_UPDATE_FAILED", "投票计数失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void rollbackIncrement(Long pollId, List<Long> optionIds) {
        List<String> args = optionIds.stream().map(String::valueOf).toList();
        Object[] scriptArgs = args.toArray();
        redisTemplate.execute(
                VOTE_DECREMENT_SCRIPT,
                List.of(VoteRedisKeys.pollCounts(pollId)),
                scriptArgs
        );
    }

    public Map<Long, Long> pollOptionCounts(Long pollId, List<Long> optionIds) {
        List<Object> hashKeys = optionIds.stream()
                .map(String::valueOf)
                .map(key -> (Object) key)
                .toList();
        List<Object> values = redisTemplate.opsForHash()
                .multiGet(VoteRedisKeys.pollCounts(pollId), hashKeys);
        Map<Long, Long> result = new LinkedHashMap<>();
        for (int i = 0; i < optionIds.size(); i++) {
            Object value = values == null ? null : values.get(i);
            result.put(optionIds.get(i), toLong(value));
        }
        return result;
    }

    public long pollBallots(Long pollId) {
        Object ballots = redisTemplate.opsForHash().get(VoteRedisKeys.pollCounts(pollId), BALLOTS_FIELD);
        return toLong(ballots);
    }

    public void replayVote(Long pollId, List<Long> optionIds) {
        for (Long optionId : optionIds) {
            redisTemplate.opsForHash().increment(VoteRedisKeys.pollCounts(pollId), String.valueOf(optionId), 1L);
        }
        redisTemplate.opsForHash().increment(VoteRedisKeys.pollCounts(pollId), BALLOTS_FIELD, 1L);
    }

    public boolean markVotedIfAbsent(Long pollId, String voterId) {
        Long added = redisTemplate.opsForSet().add(VoteRedisKeys.pollVoters(pollId), voterId);
        return Long.valueOf(1L).equals(added);
    }

    public void unmarkVoted(Long pollId, String voterId) {
        redisTemplate.opsForSet().remove(VoteRedisKeys.pollVoters(pollId), voterId);
    }

    public boolean hasVoted(Long pollId, String voterId) {
        Boolean exists = redisTemplate.opsForSet().isMember(VoteRedisKeys.pollVoters(pollId), voterId);
        return Boolean.TRUE.equals(exists);
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
