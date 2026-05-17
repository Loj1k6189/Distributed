package com.example.distributed.quest.api;

import com.example.distributed.quest.service.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/question")
@RequiredArgsConstructor
public class QuestionController {
    
    private final QuestionService questionService;
    
    // 1. 创建问卷 (POST /api/v1/questions)
    @PostMapping
    public QuestionCreateResponse createQuestion(@RequestBody @Valid QuestionCreateRequest request) {
        return questionService.createQuestion(request);
    }
    
    // 2. 获取问卷列表 (GET /api/v1/questions)
    @GetMapping
    public List<QuestionSummaryResponse> getQuestions() {
        return questionService.getQuestions();
    }
    
    // 3. 获取问卷详情 (GET /api/v1/questions/{id})
    @GetMapping("/{questionId}")
    public QuestionDetailResponse getQuestion(@PathVariable Long questionId) {
        return questionService.getQuestion(questionId);
    }
    
    // 4. 提交问卷 (POST /api/v1/question/submit)
    @PostMapping("/submit")
    public QuestionSubmitResponse submitQuestion(@RequestBody @Valid QuestionSubmitRequest request, 
                                                HttpServletRequest httpServletRequest) {
        return questionService.submitQuestion(request, getClientIp(httpServletRequest));
    }
    
    // 5. 查询用户提交记录 (GET /api/v1/question/submitted)
    @GetMapping("/submitted")
    public QuestionSubmittedResponse checkSubmission(@RequestParam String userId, 
                                                    @RequestParam Long questionId) {
        return questionService.checkSubmission(userId, questionId);
    }
    
    // 6. 查询实时统计 (GET /api/v1/question/stats/{id})
    @GetMapping("/stats/{questionId}")
    public QuestionStatsResponse getQuestionStats(@PathVariable Long questionId) {
        return questionService.getQuestionStats(questionId);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}