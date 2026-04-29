package com.example.distributed.lottery.api;

import java.time.Instant;

public record DrawWinnerResponse(
        Long campaignId,
        Integer roundNo,
        String userId,
        String displayName,
        Instant drawnAt
) {
}
