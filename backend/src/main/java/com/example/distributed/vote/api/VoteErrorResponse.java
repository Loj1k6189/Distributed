package com.example.distributed.vote.api;

import java.time.Instant;

public record VoteErrorResponse(
        String code,
        String message,
        Instant timestamp
) {
}

