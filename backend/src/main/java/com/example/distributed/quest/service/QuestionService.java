package com.example.distributed.quest.service;

import com.example.distributed.quest.api.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface QuestionService {
    QuestionCreateResponse createQuestion(QuestionCreateRequest request);

    List<QuestionSummaryResponse> getQuestions();

    QuestionDetailResponse getQuestion(Long questionId);

    QuestionSubmitResponse submitQuestion(QuestionSubmitRequest request, String clientIp);

    QuestionSubmittedResponse checkSubmission(String userId, Long questionId);

    QuestionStatsResponse getQuestionStats(Long questionId);
}