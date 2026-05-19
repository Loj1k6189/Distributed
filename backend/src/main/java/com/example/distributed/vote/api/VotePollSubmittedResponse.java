package com.example.distributed.vote.api;

public record VotePollSubmittedResponse(
        Long pollId,
        String voterId,
        boolean submitted
) {
}
