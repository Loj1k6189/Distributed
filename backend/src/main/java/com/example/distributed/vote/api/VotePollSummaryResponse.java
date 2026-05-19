package com.example.distributed.vote.api;

import com.example.distributed.vote.domain.VotePollStatus;
import java.time.Instant;

public record VotePollSummaryResponse(
        Long pollId,
        String name,
        boolean allowMultiple,
        VotePollStatus status,
        Instant createdAt
) {
}
