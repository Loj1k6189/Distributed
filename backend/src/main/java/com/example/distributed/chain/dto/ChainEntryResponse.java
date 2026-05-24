package com.example.distributed.chain.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接龙项响应DTO
 */
@Data
@Builder
public class ChainEntryResponse {

    private Long id;
    private Long chainId;
    private String userId;
    private String content;
    private Long sequenceNo;
    private Long parentEntryId;
    private LocalDateTime createdAt;
}
