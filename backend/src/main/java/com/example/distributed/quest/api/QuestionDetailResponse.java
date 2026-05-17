package com.example.distributed.quest.api;

import java.time.LocalDateTime;
import java.util.List;

public record QuestionDetailResponse(
        Long id,
        String title,
        String description,
        Boolean allowMultiple,
        Integer maxOptions,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        List<OptionInfo> options
) {
    public record OptionInfo(
            Long id,
            String optionKey,
            String optionValue
    ) {}
}