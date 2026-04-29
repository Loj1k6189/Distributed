package com.example.distributed.lottery.api;

public record JoinResponse(
        Long campaignId,
        String userId,
        String displayName,
        boolean accepted,
        String message
) {
}
