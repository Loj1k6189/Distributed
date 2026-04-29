package com.example.distributed.lottery.api;

import jakarta.validation.constraints.NotBlank;

public record JoinRequest(
        @NotBlank(message = "用户标识不能为空")
        String userId,
        @NotBlank(message = "展示名称不能为空")
        String displayName
) {
}
