package com.example.distributed.vote.api;

import java.util.List;

public record VotePollResultResponse(
        Long pollId,
        String name,
        boolean allowMultiple,
        Long ballots,
        List<VoteOptionResult> options
) {
}

