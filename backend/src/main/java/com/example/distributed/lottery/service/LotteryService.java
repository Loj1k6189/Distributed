package com.example.distributed.lottery.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.distributed.lottery.domain.LotteryHistory;
import com.example.distributed.lottery.repository.LotteryHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LotteryService {

    private final StringRedisTemplate redisTemplate;
    private final LotteryHistoryRepository historyRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String POOL_KEY_PREFIX = "lottery:pool:";
    private static final String WINNERS_KEY_PREFIX = "lottery:winners:";
    private static final String ROUND_WINNERS_KEY_PREFIX = "lottery:round_winners:";

    public void joinPool(String activityId, String userId) {
        redisTemplate.opsForSet().add(POOL_KEY_PREFIX + activityId, userId);
    }

    @Transactional
    public List<String> draw(String activityId, int round, int count) {
        String poolKey = POOL_KEY_PREFIX + activityId;
        String allWinnersKey = WINNERS_KEY_PREFIX + activityId;
        String roundWinnersKey = ROUND_WINNERS_KEY_PREFIX + activityId + ":" + round;

        List<String> currentWinners = new ArrayList<>();
        List<LotteryHistory> historyBatch = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // SPOP - atomic pop random member
            String winner = redisTemplate.opsForSet().pop(poolKey);
            if (winner == null) {
                break; // pool is empty
            }
            
            // Check if already won
            Long addedCount = redisTemplate.opsForSet().add(allWinnersKey, winner);
            if (addedCount != null && addedCount > 0) {
                currentWinners.add(winner);

                LotteryHistory history = new LotteryHistory();
                history.setActivityId(activityId);
                history.setUserId(winner);
                history.setRound(round);
                history.setWonAt(Instant.now());
                historyBatch.add(history);
            } else {
                i--; // Was already a winner somehow, retry
            }
        }

        if (!currentWinners.isEmpty()) {
            try {
                // DB 确认落库
                historyRepository.saveAll(historyBatch);
                // 成功后写入本轮中奖名单（供历史翻查和轮询）
                redisTemplate.opsForList().leftPushAll(roundWinnersKey, currentWinners);
                // 实时推送 WebSocket
                messagingTemplate.convertAndSend("/topic/lottery/winners/" + activityId, currentWinners);
            } catch (Exception e) {
                // 补偿回滚：从已中奖结合中移除，放回抽奖池
                if (!currentWinners.isEmpty()) {
                    redisTemplate.opsForSet().remove(allWinnersKey, currentWinners.toArray());
                    redisTemplate.opsForSet().add(poolKey, currentWinners.toArray(new String[0]));
                }
                throw new RuntimeException("抽奖执行落库失败，已补偿回滚奖池", e);
            }
        }

        return currentWinners;
    }

    public List<String> getLatestWinners(String activityId, int round) {
        String roundWinnersKey = ROUND_WINNERS_KEY_PREFIX + activityId + ":" + round;
        return redisTemplate.opsForList().range(roundWinnersKey, 0, -1);
    }

    public Page<LotteryHistory> getHistory(String activityId, int page, int size) {
        return historyRepository.findByActivityId(activityId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "wonAt")));
    }
}