package com.example.distributed.quest.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionEventMessage {
    private String eventId;
    private Long questionId;
    private String userId;
    private String sourceIp;
    private List<Long> optionIds;
    private Instant createdAt;
}