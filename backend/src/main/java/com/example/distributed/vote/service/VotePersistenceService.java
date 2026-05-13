package com.example.distributed.vote.service;

import com.example.distributed.vote.domain.VoteEventEntity;
import com.example.distributed.vote.repository.VoteEventRepository;
import com.example.distributed.vote.repository.VoteOptionCountRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VotePersistenceService {

    private final VoteEventRepository voteEventRepository;
    private final VoteOptionCountRepository optionCountRepository;

    @Transactional
    public void persistBatch(List<VoteEventMessage> events) {
        if (events.isEmpty()) {
            return;
        }
        List<String> incomingIds = events.stream().map(VoteEventMessage::eventId).toList();
        Set<String> existingIds = new HashSet<>(voteEventRepository.findExistingEventIds(incomingIds));
        List<VoteEventEntity> eventEntities = new ArrayList<>();
        Map<CountKey, Long> increments = new HashMap<>();
        Instant now = Instant.now();

        for (VoteEventMessage event : events) {
            if (existingIds.contains(event.eventId())) {
                continue;
            }
            VoteEventEntity entity = new VoteEventEntity();
            entity.setEventId(event.eventId());
            entity.setPollId(event.pollId());
            entity.setVoterId(event.voterId());
            entity.setSourceIp(event.sourceIp());
            entity.setOptionIds(toOptionIdString(event.optionIds()));
            entity.setCreatedAt(event.createdAt());
            eventEntities.add(entity);
            for (Long optionId : event.optionIds()) {
                CountKey key = new CountKey(event.pollId(), optionId);
                increments.merge(key, 1L, Long::sum);
            }
        }

        if (eventEntities.isEmpty()) {
            return;
        }
        voteEventRepository.saveAll(eventEntities);
        for (Map.Entry<CountKey, Long> increment : increments.entrySet()) {
            optionCountRepository.upsertCount(
                    increment.getKey().pollId(),
                    increment.getKey().optionId(),
                    increment.getValue(),
                    now
            );
        }
    }

    private String toOptionIdString(List<Long> optionIds) {
        return optionIds.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("");
    }

    private record CountKey(Long pollId, Long optionId) {
    }
}

