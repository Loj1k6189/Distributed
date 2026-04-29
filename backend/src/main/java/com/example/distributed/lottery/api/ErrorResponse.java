package com.example.distributed.lottery.api;

public record ErrorResponse(
        String code,
        String message
) {
}
