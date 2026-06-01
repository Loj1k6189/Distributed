package com.example.distributed.chain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接龙创建请求DTO
 */
@Data
public class ChainCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200字符")
    private String title;

    @Size(max = 1000, message = "描述长度不能超过1000字符")
    private String description;

    private Integer maxParticipants;

    private Boolean allowMultiple = false;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
