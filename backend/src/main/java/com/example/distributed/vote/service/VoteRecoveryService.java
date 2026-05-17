package com.example.distributed.vote.service;

import com.example.distributed.vote.config.VoteProperties;
import com.example.distributed.vote.domain.VoteCountSnapshot;
import com.example.distributed.vote.domain.VoteEventEntity;
import com.example.distributed.vote.domain.VoteOption;
import com.example.distributed.vote.domain.VoteOptionCount;
import com.example.distributed.vote.domain.VotePoll;
import com.example.distributed.vote.repository.VoteCountSnapshotRepository;
import com.example.distributed.vote.repository.VoteEventRepository;
import com.example.distributed.vote.repository.VoteOptionCountRepository;
import com.example.distributed.vote.repository.VoteOptionRepository;
import com.example.distributed.vote.repository.VotePollRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteRecoveryService {

    private static final long BALLOTS_OPTION_ID = 0L;

    private final VotePollRepository pollRepository;
    private final VoteOptionRepository optionRepository;
    private final VoteOptionCountRepository optionCountRepository;
    private final VoteCountSnapshotRepository snapshotRepository;
    private final VoteEventRepository eventRepository;
    private final RedisVoteCounter redisVoteCounter;
    private final VoteProperties voteProperties;

    @Transactional
    @Scheduled(fixedDelayString = "${app.vote.snapshot-delay-ms:300000}")
    public void createSnapshot() {
        if (!voteProperties.isSnapshotEnabled()) {
            return;
        }
        Instant snapshotAt = Instant.now();
        List<VoteOptionCount> counts = optionCountRepository.findAll();
        Map<Long, Long> pollBallots = new HashMap<>();
        for (VoteOptionCount count : counts) {
            snapshotRepository.upsertSnapshot(count.getPollId(), count.getOptionId(), count.getVoteCount(), snapshotAt);
            pollBallots.computeIfAbsent(count.getPollId(), eventRepository::countByPollId);
        }
        pollBallots.forEach((pollId, ballots) ->
                snapshotRepository.upsertSnapshot(pollId, BALLOTS_OPTION_ID, ballots, snapshotAt));
    }

    @Transactional(readOnly = true)
    public int rebuildAllFromSnapshotAndReplay() {
        List<VotePoll> polls = pollRepository.findAll();
        int rebuilt = 0;
        for (VotePoll poll : polls) {
            Long pollId = poll.getId();
            List<VoteOption> options = optionRepository.findByPollIdOrderBySortNoAsc(pollId);
            if (options.isEmpty()) {
                continue;
            }
            List<Long> optionIds = options.stream().map(VoteOption::getId).toList();
            List<VoteCountSnapshot> snapshots = snapshotRepository.findByPollId(pollId);
            Instant replayStart = Instant.EPOCH;
            Map<Long, Long> optionCounts = new HashMap<>();
            long ballots = 0L;
            if (!snapshots.isEmpty()) {
                for (VoteCountSnapshot snapshot : snapshots) {
                    replayStart = replayStart.isAfter(snapshot.getSnapshotAt()) ? replayStart : snapshot.getSnapshotAt();
                    if (snapshot.getOptionId().equals(BALLOTS_OPTION_ID)) {
                        ballots = snapshot.getVoteCount();
                    } else {
                        optionCounts.put(snapshot.getOptionId(), snapshot.getVoteCount());
                    }
                }
            } else {
                List<VoteOptionCount> current = optionCountRepository.findByPollId(pollId);
                for (VoteOptionCount optionCount : current) {
                    optionCounts.put(optionCount.getOptionId(), optionCount.getVoteCount());
                }
                ballots = eventRepository.countByPollId(pollId);
            }
            redisVoteCounter.setPollCounts(pollId, optionIds, optionCounts, ballots);
            if (!snapshots.isEmpty()) {
                List<VoteEventEntity> increments = eventRepository.findByPollIdAndCreatedAtAfterOrderByCreatedAtAsc(pollId, replayStart);
                for (VoteEventEntity increment : increments) {
                    redisVoteCounter.replayVote(pollId, parseOptionIds(increment.getOptionIds()));
                }
            }
            rebuilt++;
        }
        return rebuilt;
    }

    private List<Long> parseOptionIds(String optionIds) {
        return java.util.Arrays.stream(optionIds.split(","))
                .filter(value -> !value.isBlank())
                .map(String::trim)
                .map(Long::valueOf)
                .toList();
    }
}
