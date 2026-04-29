package com.example.distributed.lottery.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCampaignRequest(
        @NotBlank(message = "活动名称不能为空")
        String name,
        @NotNull(message = "中奖人数不能为空")
        @Min(value = 1, message = "中奖人数至少为1")
        @Max(value = 100000, message = "中奖人数过大")
        Integer plannedWinners
) {
}
