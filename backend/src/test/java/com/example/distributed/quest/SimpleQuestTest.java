package com.example.distributed.quest;

import com.example.distributed.quest.dto.AnswerSubmissionRequest;
import com.example.distributed.quest.dto.QuestionnaireCreateRequest;
import com.example.distributed.quest.dto.QuestionnaireResponse;
import com.example.distributed.quest.dto.StatisticsResponse;
import com.example.distributed.quest.entity.Questionnaire;
import com.example.distributed.quest.enums.QuestionType;
import com.example.distributed.quest.exception.QuestionnaireException;
import com.example.distributed.quest.service.QuestionnaireService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 问卷服务简单测试
 * 使用已有的Spring Boot Test和JPA Test依赖
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SimpleQuestTest {

    @Autowired
    private QuestionnaireService questionnaireService;

    private String testUserId;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-001";
    }

    @Test
    @DisplayName("测试：创建问卷")
    void testCreateQuestionnaire() {
        // 准备测试数据
        QuestionnaireCreateRequest request = createSampleRequest();

        // 执行创建
        Questionnaire questionnaire = questionnaireService.createQuestionnaire(testUserId, request);

        // 验证结果
        assertNotNull(questionnaire);
        assertNotNull(questionnaire.getId());
        assertEquals("测试问卷", questionnaire.getTitle());
        assertEquals(3, questionnaire.getQuestions().size());
        assertTrue(questionnaire.getIsActive());
        
        System.out.println("✓ 问卷创建成功，ID: " + questionnaire.getId());
    }

    @Test
    @DisplayName("测试：获取问卷详情")
    void testGetQuestionnaire() {
        // 先创建问卷
        QuestionnaireCreateRequest request = createSampleRequest();
        Questionnaire created = questionnaireService.createQuestionnaire(testUserId, request);

        // 获取详情
        QuestionnaireResponse response = questionnaireService.getQuestionnaire(created.getId());

        // 验证
        assertNotNull(response);
        assertEquals(created.getId(), response.getId());
        assertEquals("测试问卷", response.getTitle());
        assertNotNull(response.getQuestions());
        
        System.out.println("✓ 问卷获取成功，题目数: " + response.getQuestions().size());
    }

    @Test
    @DisplayName("测试：多种题型支持")
    void testMultipleQuestionTypes() {
        // 创建包含多种题型的问卷
        QuestionnaireCreateRequest request = createSampleRequest();
        Questionnaire questionnaire = questionnaireService.createQuestionnaire(testUserId, request);

        // 验证题型
        List<com.example.distributed.quest.entity.Question> questions = questionnaire.getQuestions();
        
        assertEquals(QuestionType.SINGLE_CHOICE, questions.get(0).getQuestionType());
        assertEquals(QuestionType.MULTIPLE_CHOICE, questions.get(1).getQuestionType());
        assertEquals(QuestionType.TEXT_ANSWER, questions.get(2).getQuestionType());
        
        System.out.println("✓ 支持多种题型：单选题、多选题、文本题");
    }

    @Test
    @DisplayName("测试：问卷不存在时抛出异常")
    void testQuestionnaireNotFound() {
        // 验证不存在的问卷会抛出异常
        assertThrows(QuestionnaireException.class, () -> {
            questionnaireService.getQuestionnaire(999999L);
        });
        
        System.out.println("✓ 异常处理正常");
    }

    @Test
    @DisplayName("测试：获取统计信息")
    void testGetStatistics() {
        // 创建问卷
        QuestionnaireCreateRequest request = createSampleRequest();
        Questionnaire questionnaire = questionnaireService.createQuestionnaire(testUserId, request);

        // 获取统计
        StatisticsResponse statistics = questionnaireService.getStatistics(questionnaire.getId());

        // 验证
        assertNotNull(statistics);
        assertEquals(questionnaire.getId(), statistics.getQuestionnaireId());
        
        System.out.println("✓ 统计功能正常");
    }

    @Test
    @DisplayName("测试：获取活跃问卷列表")
    void testGetActiveQuestionnaires() {
        // 创建问卷
        QuestionnaireCreateRequest request = createSampleRequest();
        questionnaireService.createQuestionnaire(testUserId, request);

        // 获取活跃列表
        var activeList = questionnaireService.getActiveQuestionnaires();

        // 验证
        assertNotNull(activeList);
        assertFalse(activeList.isEmpty());
        
        System.out.println("✓ 活跃问卷列表获取成功，数量: " + activeList.size());
    }

    /**
     * 创建示例问卷请求
     */
    private QuestionnaireCreateRequest createSampleRequest() {
        QuestionnaireCreateRequest request = new QuestionnaireCreateRequest();
        request.setTitle("测试问卷");
        request.setDescription("这是一个测试问卷");
        request.setAllowAnonymous(true);

        // 单选题
        QuestionnaireCreateRequest.QuestionDTO q1 = new QuestionnaireCreateRequest.QuestionDTO();
        q1.setContent("您的性别？");
        q1.setQuestionType(QuestionType.SINGLE_CHOICE);
        q1.setSortOrder(0);
        q1.setIsRequired(true);
        q1.setOptions(List.of(
                createOption("男", 0),
                createOption("女", 1)
        ));

        // 多选题
        QuestionnaireCreateRequest.QuestionDTO q2 = new QuestionnaireCreateRequest.QuestionDTO();
        q2.setContent("喜欢的功能？");
        q2.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        q2.setSortOrder(1);
        q2.setIsRequired(true);
        q2.setOptions(List.of(
                createOption("功能A", 0),
                createOption("功能B", 1),
                createOption("功能C", 2)
        ));

        // 文本题
        QuestionnaireCreateRequest.QuestionDTO q3 = new QuestionnaireCreateRequest.QuestionDTO();
        q3.setContent("您的建议？");
        q3.setQuestionType(QuestionType.TEXT_ANSWER);
        q3.setSortOrder(2);
        q3.setIsRequired(false);

        request.setQuestions(List.of(q1, q2, q3));
        return request;
    }

    /**
     * 创建选项
     */
    private QuestionnaireCreateRequest.OptionDTO createOption(String content, int sortOrder) {
        QuestionnaireCreateRequest.OptionDTO option = new QuestionnaireCreateRequest.OptionDTO();
        option.setContent(content);
        option.setSortOrder(sortOrder);
        return option;
    }
}
