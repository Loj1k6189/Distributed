package com.example.distributed.chain.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 接龙响应DTO
 */
@Data
@Builder
public class ChainResponse {

    private Long id;
    private String title;
    private String description;
    private String createdBy;
    private Integer maxParticipants;
    private Boolean allowMultiple;
    private Boolean isActive;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private Long participantCount;
    private List<ChainEntryResponse> entries;
}
