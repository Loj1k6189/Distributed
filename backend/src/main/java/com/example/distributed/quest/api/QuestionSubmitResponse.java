package com.example.distributed.quest.api;

import java.time.Instant;

public record QuestionSubmitResponse(
        String submissionId,
        Long questionId,
        Instant submittedAt
) {
}