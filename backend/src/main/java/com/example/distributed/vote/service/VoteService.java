package com.example.distributed.vote.service;

import com.example.distributed.vote.api.VoteOptionResult;
import com.example.distributed.vote.api.VotePollCreateRequest;
import com.example.distributed.vote.api.VotePollCreateResponse;
import com.example.distributed.vote.api.VotePollResultResponse;
import com.example.distributed.vote.api.VoteSubmitRequest;
import com.example.distributed.vote.api.VoteSubmitResponse;
import com.example.distributed.vote.domain.VoteOption;
import com.example.distributed.vote.domain.VoteOptionCount;
import com.example.distributed.vote.domain.VoteOutboxMessage;
import com.example.distributed.vote.domain.VotePoll;
import com.example.distributed.vote.domain.VotePollStatus;
import com.example.distributed.vote.repository.VoteEventRepository;
import com.example.distributed.vote.repository.VoteOptionCountRepository;
import com.example.distributed.vote.repository.VoteOptionRepository;
import com.example.distributed.vote.repository.VotePollRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VotePollRepository pollRepository;
    private final VoteOptionRepository optionRepository;
    private final VoteOptionCountRepository optionCountRepository;
    private final VoteEventRepository eventRepository;
    private final VoteRateLimiter voteRateLimiter;
    private final RedlockService redlockService;
    private final RedisVoteCounter redisVoteCounter;
    private final VoteOutboxService outboxService;

    @Transactional
    public VotePollCreateResponse createPoll(VotePollCreateRequest request) {
        List<String> options = normalizeOptionTexts(request.options());
        VotePoll poll = new VotePoll();
        poll.setName(request.name().trim());
        poll.setAllowMultiple(Boolean.TRUE.equals(request.allowMultiple()));
        poll.setStatus(VotePollStatus.ACTIVE);
        poll = pollRepository.save(poll);

        List<VoteOption> optionEntities = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            VoteOption option = new VoteOption();
            option.setPollId(poll.getId());
            option.setOptionText(options.get(i));
            option.setSortNo(i + 1);
            optionEntities.add(option);
        }
        List<VoteOption> savedOptions = optionRepository.saveAll(optionEntities);
        redisVoteCounter.initializePoll(poll.getId(), savedOptions.stream().map(VoteOption::getId).toList());

        return new VotePollCreateResponse(
                poll.getId(),
                poll.getName(),
                poll.isAllowMultiple(),
                savedOptions.stream().map(option -> new VoteOptionResult(option.getId(), option.getOptionText(), 0L)).toList()
        );
    }

    public VoteSubmitResponse submitVote(VoteSubmitRequest request, String sourceIp) {
        VotePoll poll = pollRepository.findById(request.pollId())
                .orElseThrow(() -> new VoteBusinessException("POLL_NOT_FOUND", "投票活动不存在", HttpStatus.NOT_FOUND));
        if (poll.getStatus() != VotePollStatus.ACTIVE) {
            throw new VoteBusinessException("POLL_CLOSED", "投票活动已关闭", HttpStatus.CONFLICT);
        }
        List<Long> optionIds = normalizeOptionIds(request.optionIds());
        if (!poll.isAllowMultiple() && optionIds.size() != 1) {
            throw new VoteBusinessException("INVALID_OPTION_COUNT", "该活动仅支持单选", HttpStatus.BAD_REQUEST);
        }
        String voterId = request.voterId().trim();
        String ip = sourceIp == null || sourceIp.isBlank() ? "unknown" : sourceIp;
        String requestId = UUID.randomUUID().toString();
        voteRateLimiter.validate(request.pollId(), voterId, ip, requestId);

        RedlockService.RedlockToken lockToken = redlockService.acquire("poll:" + request.pollId());
        try {
            redisVoteCounter.incrementVote(request.pollId(), optionIds);
            VoteEventMessage eventMessage = new VoteEventMessage(
                    UUID.randomUUID().toString(),
                    request.pollId(),
                    voterId,
                    ip,
                    optionIds,
                    Instant.now()
            );
            try {
                VoteOutboxMessage outboxMessage = outboxService.saveOutbox(eventMessage);
                outboxService.tryPublish(outboxMessage);
            } catch (RuntimeException ex) {
                redisVoteCounter.rollbackIncrement(request.pollId(), optionIds);
                throw new VoteBusinessException("EVENT_WRITE_FAILED", "事务消息保存失败，投票已回滚", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new VoteSubmitResponse(eventMessage.eventId(), request.pollId(), eventMessage.createdAt());
        } finally {
            redlockService.unlock(lockToken);
        }
    }

    public VotePollResultResponse pollResult(Long pollId) {
        VotePoll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new VoteBusinessException("POLL_NOT_FOUND", "投票活动不存在", HttpStatus.NOT_FOUND));
        List<VoteOption> options = optionRepository.findByPollIdOrderBySortNoAsc(poll.getId());
        if (options.isEmpty()) {
            throw new VoteBusinessException("POLL_OPTION_EMPTY", "投票活动未配置选项", HttpStatus.CONFLICT);
        }

        List<Long> optionIds = options.stream().map(VoteOption::getId).toList();
        Map<Long, Long> countByOption = redisVoteCounter.pollOptionCounts(poll.getId(), optionIds);
        long ballots = redisVoteCounter.pollBallots(poll.getId());
        if (countByOption.values().stream().allMatch(value -> value == 0L) && ballots == 0L) {
            Map<Long, Long> fallback = new LinkedHashMap<>();
            for (VoteOptionCount optionCount : optionCountRepository.findByPollId(poll.getId())) {
                fallback.put(optionCount.getOptionId(), optionCount.getVoteCount());
            }
            if (!fallback.isEmpty()) {
                countByOption = fallback;
                ballots = eventRepository.countByPollId(poll.getId());
            }
        }
        Map<Long, Long> finalCountByOption = countByOption;
        List<VoteOptionResult> results = options.stream()
                .map(option -> new VoteOptionResult(
                        option.getId(),
                        option.getOptionText(),
                        finalCountByOption.getOrDefault(option.getId(), 0L))
                ).toList();
        return new VotePollResultResponse(poll.getId(), poll.getName(), poll.isAllowMultiple(), ballots, results);
    }

    private List<String> normalizeOptionTexts(List<String> options) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String option : options) {
            String value = option == null ? "" : option.trim();
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        if (normalized.size() < 2) {
            throw new VoteBusinessException("INVALID_OPTIONS", "投票选项至少需要 2 个且不能重复", HttpStatus.BAD_REQUEST);
        }
        return normalized.stream().toList();
    }

    private List<Long> normalizeOptionIds(List<Long> optionIds) {
        Set<Long> normalized = new LinkedHashSet<>(optionIds);
        if (normalized.isEmpty()) {
            throw new VoteBusinessException("OPTION_REQUIRED", "至少选择一个投票选项", HttpStatus.BAD_REQUEST);
        }
        return normalized.stream().toList();
    }
}
