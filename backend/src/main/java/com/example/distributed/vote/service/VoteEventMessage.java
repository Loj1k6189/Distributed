package com.example.distributed.vote.service;

import java.time.Instant;
import java.util.List;

public record VoteEventMessage(
        String eventId,
        Long pollId,
        String voterId,
        String sourceIp,
        List<Long> optionIds,
        Instant createdAt
) {
}

