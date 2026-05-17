package com.example.distributed.vote.api;

import java.util.List;

public record VotePollCreateResponse(
        Long pollId,
        String name,
        boolean allowMultiple,
        List<VoteOptionResult> options
) {
}

