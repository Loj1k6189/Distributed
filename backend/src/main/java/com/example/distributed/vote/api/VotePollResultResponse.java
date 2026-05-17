package com.example.distributed.vote.api;

import java.util.List;

public record VotePollResultResponse(
        Long pollId,
        Long ballots,
        List<VoteOptionResult> options
) {
}

