package com.example.distributed.quest.api;

import java.time.Instant;
import java.util.List;

public record QuestionStatsResponse(
        Long questionId,
        String questionTitle,
        Long totalSubmissions,
        List<OptionStats> options,
        Instant lastUpdated
) {
    public record OptionStats(
            Long optionId,
            String optionText,
            Long votes,
            Double percentage,
            Integer rank
    ) {}
}