package com.example.distributed.quest.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 答卷提交请求DTO
 */
@Data
public class AnswerSubmissionRequest {

    @NotNull(message = "问卷ID不能为空")
    private Long questionnaireId;

    private Boolean isAnonymous = false;

    private Long startTime;

    @NotNull(message = "答案列表不能为空")
    private List<AnswerDTO> answers;

    @Data
    public static class AnswerDTO {
        @NotNull(message = "题目ID不能为空")
        private Long questionId;

        private String textAnswer;

        private List<Long> selectedOptionIds;
    }
}
