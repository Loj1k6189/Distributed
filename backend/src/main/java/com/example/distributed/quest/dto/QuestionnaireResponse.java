package com.example.distributed.quest.dto;

import com.example.distributed.quest.enums.QuestionType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问卷响应DTO
 */
@Data
public class QuestionnaireResponse {

    private Long id;
    private String title;
    private String description;
    private Boolean isActive;
    private Integer maxSubmissions;
    private Boolean allowAnonymous;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<QuestionResponse> questions;

    @Data
    public static class QuestionResponse {
        private Long id;
        private String content;
        private QuestionType questionType;
        private Integer sortOrder;
        private Boolean isRequired;
        private List<OptionResponse> options;
    }

    @Data
    public static class OptionResponse {
        private Long id;
        private String content;
        private Integer sortOrder;
    }
}
