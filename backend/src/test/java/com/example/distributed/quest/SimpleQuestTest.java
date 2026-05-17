package com.example.distributed.quest;

import com.example.distributed.quest.api.*;
import com.example.distributed.quest.service.QuestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单的问卷功能测试，验证基本功能是否可用
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:3306/vote_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
    "spring.datasource.username=root",
    "spring.datasource.password=123456",
    "spring.jpa.hibernate.ddl-auto=update"
})
class SimpleQuestTest {

    @Autowired
    private QuestionService questionService;

    @Test
    void testQuestionLifecycle() {
        System.out.println("开始测试问卷功能...");

        // 1. 创建问卷
        System.out.println("1. 创建问卷中...");
        QuestionCreateRequest createRequest = new QuestionCreateRequest(
                "测试问卷",
                "这是一个用于验证功能的测试问卷",
                true, // 允许多选
                Arrays.asList("选项1", "选项2", "选项3", "选项4")
        );

        QuestionCreateResponse createResponse = questionService.createQuestion(createRequest);
        assertNotNull(createResponse.id(), "问卷创建失败，ID为空");
        System.out.println("   问卷创建成功，ID: " + createResponse.id());

        // 2. 获取问卷列表
        System.out.println("2. 获取问卷列表...");
        List<QuestionSummaryResponse> questions = questionService.getQuestions();
        assertTrue(questions.size() > 0, "问卷列表为空");
        System.out.println("   问卷列表获取成功，当前问卷数量: " + questions.size());

        // 3. 获取问卷详情
        System.out.println("3. 获取问卷详情...");
        QuestionDetailResponse detail = questionService.getQuestion(createResponse.id());
        assertEquals(createResponse.id(), detail.id(), "问卷ID不匹配");
        assertEquals(4, detail.options().size(), "选项数量不正确");
        System.out.println("   问卷详情获取成功，选项数量: " + detail.options().size());

        // 4. 提交问卷
        System.out.println("4. 提交问卷...");
        QuestionSubmitRequest submitRequest = new QuestionSubmitRequest(
                createResponse.id(),
                "test_user_001",
                Arrays.asList(detail.options().get(0).id(), detail.options().get(1).id()) // 选择前两个选项
        );

        QuestionSubmitResponse submitResponse = questionService.submitQuestion(submitRequest, "127.0.0.1");
        assertNotNull(submitResponse.submissionId(), "问卷提交失败");
        System.out.println("   问卷提交成功，提交ID: " + submitResponse.submissionId());

        // 5. 检查提交状态
        System.out.println("5. 检查提交状态...");
        QuestionSubmittedResponse submitted = questionService.checkSubmission("test_user_001", createResponse.id());
        assertTrue(submitted.submitted(), "提交状态检查失败");
        System.out.println("   提交状态检查成功");

        // 6. 获取统计信息
        System.out.println("6. 获取统计信息...");
        QuestionStatsResponse stats = questionService.getQuestionStats(createResponse.id());
        assertEquals(createResponse.id(), stats.questionId(), "统计信息问卷ID不匹配");
        assertTrue(stats.totalSubmissions() > 0, "统计票数应大于0");
        System.out.println("   统计信息获取成功，总票数: " + stats.totalSubmissions());

        System.out.println("问卷功能测试完成！所有功能均正常工作。");
    }

    @Test
    void testDuplicateSubmissionPrevention() {
        System.out.println("测试防重复提交功能...");

        // 创建问卷
        QuestionCreateRequest createRequest = new QuestionCreateRequest(
                "防重复提交测试",
                "测试防重复提交功能",
                false, // 单选
                Arrays.asList("选项A", "选项B")
        );

        QuestionCreateResponse createResponse = questionService.createQuestion(createRequest);
        assertNotNull(createResponse.id());

        // 获取选项ID
        QuestionDetailResponse detail = questionService.getQuestion(createResponse.id());
        Long optionId = detail.options().get(0).id();

        // 第一次提交
        QuestionSubmitRequest firstSubmit = new QuestionSubmitRequest(
                createResponse.id(),
                "duplicate_test_user",
                Arrays.asList(optionId)
        );

        QuestionSubmitResponse firstResponse = questionService.submitQuestion(firstSubmit, "127.0.0.1");
        assertNotNull(firstResponse.submissionId());

        // 尝试重复提交，应该抛出异常
        QuestionSubmitRequest secondSubmit = new QuestionSubmitRequest(
                createResponse.id(),
                "duplicate_test_user", // 相同用户
                Arrays.asList(optionId) // 相同问卷
        );

        Exception exception = assertThrows(RuntimeException.class, () -> {
            questionService.submitQuestion(secondSubmit, "127.0.0.1");
        });

        System.out.println("防重复提交功能测试成功！");
    }
}