package com.example.distributed.vote.api;

import java.time.Instant;

public record VoteSubmitResponse(
        String eventId,
        Long pollId,
        Instant acceptedAt
) {
}

