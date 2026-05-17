package com.example.distributed.quest.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QuestionSubmitRequest(
        @NotNull Long questionId,
        @NotBlank String userId,
        @NotEmpty List<@NotNull Long> optionIds
) {
}