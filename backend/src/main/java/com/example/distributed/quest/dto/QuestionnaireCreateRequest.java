package com.example.distributed.quest.dto;

import com.example.distributed.quest.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问卷创建请求DTO
 */
@Data
public class QuestionnaireCreateRequest {

    @NotBlank(message = "问卷标题不能为空")
    private String title;

    private String description;

    private Integer maxSubmissions;

    private Boolean allowAnonymous = false;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private List<QuestionDTO> questions;

    @Data
    public static class QuestionDTO {
        @NotBlank(message = "题目内容不能为空")
        private String content;

        @NotNull(message = "题目类型不能为空")
        private QuestionType questionType;

        private Integer sortOrder = 0;

        private Boolean isRequired = true;

        private String validation;

        private List<OptionDTO> options;
    }

    @Data
    public static class OptionDTO {
        @NotBlank(message = "选项内容不能为空")
        private String content;

        private Integer sortOrder = 0;

        private Boolean isCorrect;
    }
}
