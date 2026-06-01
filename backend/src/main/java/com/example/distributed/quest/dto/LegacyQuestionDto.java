package com.example.distributed.quest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public class LegacyQuestionDto {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "问卷标题不能为空")
        private String title;

        private String description;

        private Boolean allowMultiple = false;

        @NotEmpty(message = "选项不能为空")
        @Size(min = 2, message = "至少需要两个选项")
        private List<@NotBlank(message = "选项不能为空") String> options;
    }

    @Data
    public static class CreateResponse {
        private Long id;
    }

    @Data
    public static class ListItem {
        private Long id;
        private String title;
        private String description;
        private Boolean isActive;
        private String createdAt;
        private String expiresAt;
    }

    @Data
    public static class DetailResponse {
        private Long id;
        private String title;
        private String description;
        private Boolean allowMultiple;
        private List<OptionResponse> options;
    }

    @Data
    public static class OptionResponse {
        private Long id;
        private String optionValue;
    }

    @Data
    public static class SubmitRequest {
        @NotNull(message = "问卷ID不能为空")
        private Long questionId;

        @NotBlank(message = "用户ID不能为空")
        private String userId;

        @NotEmpty(message = "选项不能为空")
        private List<Long> optionIds;
    }

    @Data
    public static class SubmitResponse {
        private boolean success;
        private Long answerId;
    }

    @Data
    public static class SubmittedResponse {
        private boolean submitted;
    }

    @Data
    public static class StatsResponse {
        private String questionTitle;
        private Long totalSubmissions;
        private String lastUpdated;
        private List<OptionStats> options;
    }

    @Data
    public static class OptionStats {
        private Long optionId;
        private String optionText;
        private Long votes;
        private Double percentage;
        private Integer rank;
    }
}
