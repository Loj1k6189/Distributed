package com.example.distributed.quest.api;

public record QuestionCreateResponse(
        Long id,
        String title,
        String description,
        Boolean allowMultiple
) {
}