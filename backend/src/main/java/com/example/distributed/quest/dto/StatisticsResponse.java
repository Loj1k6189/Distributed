package com.example.distributed.quest.dto;

import lombok.Data;

/**
 * 问卷统计响应DTO
 */
@Data
public class StatisticsResponse {
    private Long questionnaireId;
    private Long totalSubmissions;
    private Long completedSubmissions;
    private Long partialSubmissions;
    private Double averageCompletionTime;
    private Long uniqueUsers;
    private Long anonymousSubmissions;
    private String lastSubmissionAt;
    private String snapshotDate;
}
