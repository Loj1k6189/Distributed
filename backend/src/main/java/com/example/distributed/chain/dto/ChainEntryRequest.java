package com.example.distributed.chain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 接龙参与请求DTO
 */
@Data
public class ChainEntryRequest {

    @NotBlank(message = "接龙内容不能为空")
    @Size(max = 1000, message = "接龙内容长度不能超过1000字符")
    private String content;

    private Long parentEntryId;
}
