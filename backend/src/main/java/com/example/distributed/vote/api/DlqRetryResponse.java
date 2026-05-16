package com.example.distributed.vote.api;

public record DlqRetryResponse(
        int retriedCount
) {
}

