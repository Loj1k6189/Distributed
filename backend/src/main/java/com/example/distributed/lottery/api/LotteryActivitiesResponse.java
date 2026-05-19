package com.example.distributed.lottery.api;

import java.util.List;

public record LotteryActivitiesResponse(
        List<String> activities,
        String currentActivityId
) {
}
