package com.example.distributed.lottery.service;

import com.example.distributed.lottery.api.CampaignResponse;
import com.example.distributed.lottery.api.CreateCampaignRequest;
import com.example.distributed.lottery.api.DrawWinnerResponse;
import com.example.distributed.lottery.api.JoinRequest;
import com.example.distributed.lottery.api.JoinResponse;
import com.example.distributed.lottery.domain.CampaignStatus;
import com.example.distributed.lottery.domain.LotteryCampaign;
import com.example.distributed.lottery.domain.LotteryParticipant;
import com.example.distributed.lottery.domain.LotteryWinner;
import com.example.distributed.lottery.repository.LotteryCampaignRepository;
import com.example.distributed.lottery.repository.LotteryParticipantRepository;
import com.example.distributed.lottery.repository.LotteryWinnerRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class LotteryService {

    private static final Duration JOIN_MARK_EXPIRE = Duration.ofDays(7);
    private static final Duration DRAW_LOCK_EXPIRE = Duration.ofSeconds(5);

    private final LotteryCampaignRepository campaignRepository;
    private final LotteryParticipantRepository participantRepository;
    private final LotteryWinnerRepository winnerRepository;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final SimpMessagingTemplate messagingTemplate;
    private final TransactionTemplate transactionTemplate;
    @Value("${app.lottery.state-store:memory}")
    private String stateStore;

    private final ConcurrentMap<String, String> localJoinMarks = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Deque<String>> localPools = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ReentrantLock> localDrawLocks = new ConcurrentHashMap<>();

    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request) {
        LotteryCampaign campaign = new LotteryCampaign();
        campaign.setName(request.name().trim());
        campaign.setPlannedWinners(request.plannedWinners());
        campaign.setWinnersDrawn(0);
        campaign.setStatus(CampaignStatus.ACTIVE);
        LotteryCampaign saved = campaignRepository.save(campaign);
        return toCampaignResponse(saved);
    }

    @Transactional
    public JoinResponse joinCampaign(Long campaignId, JoinRequest request) {
        LotteryCampaign campaign = getCampaignOrThrow(campaignId);
        if (campaign.getStatus() != CampaignStatus.ACTIVE) {
            throw new LotteryBusinessException("CAMPAIGN_FINISHED", "当前活动已结束", HttpStatus.CONFLICT);
        }

        String userId = request.userId().trim();
        String displayName = request.displayName().trim();
        if (!markJoinIfAbsent(campaignId, userId)) {
            return new JoinResponse(campaignId, userId, displayName, false, "重复报名已忽略");
        }

        LotteryParticipant participant = new LotteryParticipant();
        participant.setCampaignId(campaignId);
        participant.setUserId(userId);
        participant.setDisplayName(displayName);
        participant.setJoinedAt(Instant.now());
        try {
            participantRepository.saveAndFlush(participant);
        } catch (DataIntegrityViolationException ex) {
            return new JoinResponse(campaignId, userId, displayName, false, "重复报名已忽略");
        }

        pushToPool(campaignId, userId);
        return new JoinResponse(campaignId, userId, displayName, true, "报名成功");
    }

    public CampaignResponse getCampaign(Long campaignId) {
        return toCampaignResponse(getCampaignOrThrow(campaignId));
    }

    public List<DrawWinnerResponse> listWinners(Long campaignId) {
        getCampaignOrThrow(campaignId);
        return winnerRepository.findByCampaignIdOrderByRoundNoAsc(campaignId).stream()
                .map(this::toWinnerResponse)
                .toList();
    }

    public DrawWinnerResponse drawWinner(Long campaignId) {
        String lockKey = drawLockKey(campaignId);
        String lockValue = UUID.randomUUID().toString();
        if (!tryAcquireDrawLock(campaignId, lockKey, lockValue)) {
            throw new LotteryBusinessException("DRAW_IN_PROGRESS", "抽奖进行中，请稍后重试", HttpStatus.CONFLICT);
        }

        try {
            DrawWinnerResponse winner = transactionTemplate.execute(status -> drawWinnerTransactional(campaignId));
            if (winner == null) {
                throw new LotteryBusinessException("DRAW_FAILED", "抽奖失败，请重试", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            messagingTemplate.convertAndSend("/topic/lottery/" + campaignId + "/winners", winner);
            return winner;
        } finally {
            releaseDrawLock(campaignId, lockKey, lockValue);
        }
    }

    private DrawWinnerResponse drawWinnerTransactional(Long campaignId) {
        LotteryCampaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new LotteryBusinessException("CAMPAIGN_NOT_FOUND", "活动不存在", HttpStatus.NOT_FOUND));

        if (campaign.getStatus() == CampaignStatus.FINISHED || campaign.getWinnersDrawn() >= campaign.getPlannedWinners()) {
            campaign.setStatus(CampaignStatus.FINISHED);
            throw new LotteryBusinessException("CAMPAIGN_FINISHED", "当前活动已结束", HttpStatus.CONFLICT);
        }

        LotteryParticipant participant = pickNextParticipant(campaignId);
        int roundNo = campaign.getWinnersDrawn() + 1;

        LotteryWinner winner = new LotteryWinner();
        winner.setCampaignId(campaignId);
        winner.setRoundNo(roundNo);
        winner.setUserId(participant.getUserId());
        winner.setDisplayName(participant.getDisplayName());
        winner.setDrawnAt(Instant.now());
        try {
            winnerRepository.saveAndFlush(winner);
        } catch (DataIntegrityViolationException ex) {
            throw new LotteryBusinessException("DRAW_CONFLICT", "中奖结果写入冲突，请重试", HttpStatus.CONFLICT);
        }

        campaign.setWinnersDrawn(roundNo);
        if (roundNo >= campaign.getPlannedWinners()) {
            campaign.setStatus(CampaignStatus.FINISHED);
        }
        campaignRepository.save(campaign);
        return toWinnerResponse(winner);
    }

    private LotteryParticipant pickNextParticipant(Long campaignId) {
        for (int i = 0; i < 100; i++) {
            String userId = popFromPool(campaignId);
            if (userId == null) {
                break;
            }
            LotteryParticipant participant = participantRepository.findByCampaignIdAndUserId(campaignId, userId).orElse(null);
            if (participant != null) {
                return participant;
            }
        }
        throw new LotteryBusinessException("NO_PARTICIPANT", "暂无可抽取的参与者", HttpStatus.CONFLICT);
    }

    private void releaseDrawLock(Long campaignId, String lockKey, String lockValue) {
        StringRedisTemplate redis = redisTemplate();
        if (useRedisStore() && redis != null) {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redis.execute(connection -> connection.scriptingCommands()
                    .eval(script.getBytes(), ReturnType.INTEGER, 1, lockKey.getBytes(), lockValue.getBytes()), true);
            return;
        }
        ReentrantLock lock = localDrawLocks.get(campaignId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    private LotteryCampaign getCampaignOrThrow(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new LotteryBusinessException("CAMPAIGN_NOT_FOUND", "活动不存在", HttpStatus.NOT_FOUND));
    }

    private CampaignResponse toCampaignResponse(LotteryCampaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getStatus().name(),
                campaign.getPlannedWinners(),
                campaign.getWinnersDrawn()
        );
    }

    private DrawWinnerResponse toWinnerResponse(LotteryWinner winner) {
        return new DrawWinnerResponse(
                winner.getCampaignId(),
                winner.getRoundNo(),
                winner.getUserId(),
                winner.getDisplayName(),
                winner.getDrawnAt()
        );
    }

    private String joinKey(Long campaignId, String userId) {
        return "lottery:campaign:" + campaignId + ":joined:" + userId;
    }

    private String poolKey(Long campaignId) {
        return "lottery:campaign:" + campaignId + ":pool";
    }

    private String drawLockKey(Long campaignId) {
        return "lottery:campaign:" + campaignId + ":draw:lock";
    }

    private boolean markJoinIfAbsent(Long campaignId, String userId) {
        StringRedisTemplate redis = redisTemplate();
        if (useRedisStore() && redis != null) {
            Boolean firstJoin = redis.opsForValue().setIfAbsent(joinKey(campaignId, userId), "1", JOIN_MARK_EXPIRE);
            return Boolean.TRUE.equals(firstJoin);
        }
        return localJoinMarks.putIfAbsent(joinKey(campaignId, userId), "1") == null;
    }

    private void pushToPool(Long campaignId, String userId) {
        StringRedisTemplate redis = redisTemplate();
        if (useRedisStore() && redis != null) {
            redis.opsForList().rightPush(poolKey(campaignId), userId);
            return;
        }
        localPools.computeIfAbsent(campaignId, ignored -> new ConcurrentLinkedDeque<>()).addLast(userId);
    }

    private String popFromPool(Long campaignId) {
        StringRedisTemplate redis = redisTemplate();
        if (useRedisStore() && redis != null) {
            return redis.opsForList().leftPop(poolKey(campaignId));
        }
        Deque<String> deque = localPools.get(campaignId);
        return deque == null ? null : deque.pollFirst();
    }

    private boolean tryAcquireDrawLock(Long campaignId, String lockKey, String lockValue) {
        StringRedisTemplate redis = redisTemplate();
        if (useRedisStore() && redis != null) {
            Boolean locked = redis.opsForValue().setIfAbsent(lockKey, lockValue, DRAW_LOCK_EXPIRE);
            return Boolean.TRUE.equals(locked);
        }
        ReentrantLock lock = localDrawLocks.computeIfAbsent(campaignId, ignored -> new ReentrantLock());
        return lock.tryLock();
    }

    private boolean useRedisStore() {
        return "redis".equalsIgnoreCase(stateStore);
    }

    private StringRedisTemplate redisTemplate() {
        return redisTemplateProvider.getIfAvailable();
    }
}
