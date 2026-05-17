package com.example.distributed.vote.api;

public record VoteOptionResult(
        Long optionId,
        String optionText,
        Long votes
) {
}

