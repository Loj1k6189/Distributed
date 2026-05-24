package com.example.distributed.quest.controller;

import com.example.distributed.quest.dto.AnswerSubmissionRequest;
import com.example.distributed.quest.dto.QuestionnaireCreateRequest;
import com.example.distributed.quest.dto.QuestionnaireResponse;
import com.example.distributed.quest.dto.StatisticsResponse;
import com.example.distributed.quest.entity.Questionnaire;
import com.example.distributed.quest.service.QuestionnaireService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 问卷控制器
 */
@RestController
@RequestMapping("/api/questionnaires")
@RequiredArgsConstructor
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    /**
     * 创建问卷
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createQuestionnaire(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @Valid @RequestBody QuestionnaireCreateRequest request) {
        
        Questionnaire questionnaire = questionnaireService.createQuestionnaire(userId, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "问卷创建成功");
        response.put("data", Map.of("id", questionnaire.getId()));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取问卷详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getQuestionnaire(@PathVariable Long id) {
        QuestionnaireResponse response = questionnaireService.getQuestionnaire(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有活跃问卷
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveQuestionnaires() {
        List<QuestionnaireResponse> questionnaires = questionnaireService.getActiveQuestionnaires();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", questionnaires);
        
        return ResponseEntity.ok(result);
    }

    /**
     * 提交答卷
     */
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitAnswer(
            @RequestHeader(value = "X-User-Id", defaultValue = "anonymous") String userId,
            @RequestHeader(value = "X-Forwarded-For", required = false) String userIp,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @Valid @RequestBody AnswerSubmissionRequest request) {
        
        if (userIp == null) {
            userIp = "127.0.0.1";
        }
        if (userAgent == null) {
            userAgent = "unknown";
        }

        Long answerId = questionnaireService.submitAnswer(userId, userIp, userAgent, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "答卷提交成功");
        response.put("data", Map.of("answerId", answerId));
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 获取问卷统计
     */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@PathVariable Long id) {
        StatisticsResponse statistics = questionnaireService.getStatistics(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", statistics);
        
        return ResponseEntity.ok(result);
    }
}
