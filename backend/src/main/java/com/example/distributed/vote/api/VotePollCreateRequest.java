package com.example.distributed.vote.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VotePollCreateRequest(
        @NotBlank @Size(max = 128) String name,
        @NotNull Boolean allowMultiple,
        @NotEmpty @Size(min = 2, max = 30) List<@NotBlank @Size(max = 128) String> options
) {
}

