package com.example.distributed.lottery.api;

public record CampaignResponse(
        Long campaignId,
        String name,
        String status,
        Integer plannedWinners,
        Integer winnersDrawn
) {
}
