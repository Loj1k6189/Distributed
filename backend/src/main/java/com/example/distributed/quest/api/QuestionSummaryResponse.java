package com.example.distributed.quest.api;

import java.time.LocalDateTime;

public record QuestionSummaryResponse(
        Long id,
        String title,
        String description,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {
}