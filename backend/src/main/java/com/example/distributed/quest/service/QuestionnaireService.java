package com.example.distributed.quest.service;

import com.example.distributed.quest.dto.AnswerSubmissionRequest;
import com.example.distributed.quest.dto.QuestionnaireCreateRequest;
import com.example.distributed.quest.dto.QuestionnaireResponse;
import com.example.distributed.quest.dto.StatisticsResponse;
import com.example.distributed.quest.entity.*;
import com.example.distributed.quest.exception.QuestionnaireException;
import com.example.distributed.quest.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 问卷核心业务服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireService {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final QuestionnaireAnswerRepository answerRepository;
    private final QuestionnaireEventRepository eventRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final QuestionnaireStatisticsRepository statisticsRepository;
    private final DistributedLockService lockService;
    private final RedisCounterService counterService;
    private final EntityManager entityManager;

    private static final Duration SUBMISSION_COUNTER_TTL = Duration.ofDays(30);

    /**
     * 创建问卷
     */
    @Transactional
    @CacheEvict(value = "questionnaires", allEntries = true)
    public Questionnaire createQuestionnaire(String userId, QuestionnaireCreateRequest request) {
        log.info("创建问卷: userId={}, title={}", userId, request.getTitle());

        // 检查是否已存在同名问卷
        if (questionnaireRepository.existsByCreatedByAndTitle(userId, request.getTitle())) {
            throw new QuestionnaireException(
                    QuestionnaireException.ErrorCode.DUPLICATE_SUBMISSION,
                    "您已创建过同名问卷");
        }

        Questionnaire questionnaire = Questionnaire.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .createdBy(userId)
                .isActive(true)
                .allowAnonymous(request.getAllowAnonymous() != null ? request.getAllowAnonymous() : false)
                .maxSubmissions(request.getMaxSubmissions())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        // 添加题目
        if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
            for (QuestionnaireCreateRequest.QuestionDTO qDTO : request.getQuestions()) {
                Question question = Question.builder()
                        .content(qDTO.getContent())
                        .questionType(qDTO.getQuestionType())
                        .sortOrder(qDTO.getSortOrder() != null ? qDTO.getSortOrder() : 0)
                        .isRequired(qDTO.getIsRequired() != null ? qDTO.getIsRequired() : true)
                        .validation(qDTO.getValidation())
                        .build();

                // 添加选项
                if (qDTO.getOptions() != null && !qDTO.getOptions().isEmpty()) {
                    for (QuestionnaireCreateRequest.OptionDTO oDTO : qDTO.getOptions()) {
                        QuestionOption option = QuestionOption.builder()
                                .content(oDTO.getContent())
                                .sortOrder(oDTO.getSortOrder() != null ? oDTO.getSortOrder() : 0)
                                .isCorrect(oDTO.getIsCorrect())
                                .build();
                        question.addOption(option);
                    }
                }

                questionnaire.addQuestion(question);
            }
        }

        Questionnaire saved = questionnaireRepository.save(questionnaire);
        
        // 记录事件
        createEvent("QUESTIONNAIRE_CREATED", saved.getId(), null, userId, 
                String.format("创建问卷: %s", saved.getTitle()));

        // 创建出站消息
        createOutboxMessage("Questionnaire", saved.getId(), "QUESTIONNAIRE_CREATED",
                String.format("{\"questionnaireId\":%d,\"title\":\"%s\"}", saved.getId(), saved.getTitle()));

        log.info("问卷创建成功: id={}", saved.getId());
        return saved;
    }

    /**
     * 获取问卷详情（带缓存和事务）
     * 注意：使用分步查询避免MultipleBagFetchException，不使用@EntityGraph
     */
    @Cacheable(value = "questionnaires", key = "#id")
    @Transactional
    public QuestionnaireResponse getQuestionnaire(Long id) {
        // 分步查询避免MultipleBagFetchException
        Questionnaire questionnaire = questionnaireRepository.findByIdBasic(id)
                .orElseThrow(() -> new QuestionnaireException(QuestionnaireException.ErrorCode.QUESTIONNAIRE_NOT_FOUND));
        
        // 手动初始化questions集合（触发LAZY加载）
        questionnaire.getQuestions().size();
        
        // 手动初始化每个question的options集合
        questionnaire.getQuestions().forEach(q -> q.getOptions().size());

        return toQuestionnaireResponse(questionnaire);
    }

    /**
     * 从数据库加载问卷（带缓存 - 内部方法）
     */
    @Cacheable(value = "questionnaires", key = "#id")
    @Transactional
    protected QuestionnaireResponse loadQuestionnaireFromDb(Long id) {
        // 分步查询避免MultipleBagFetchException
        Questionnaire questionnaire = questionnaireRepository.findByIdBasic(id)
                .orElseThrow(() -> new QuestionnaireException(QuestionnaireException.ErrorCode.QUESTIONNAIRE_NOT_FOUND));
        
        // 手动初始化questions集合（触发LAZY加载）
        questionnaire.getQuestions().size();
        
        // 手动初始化每个question的options集合
        questionnaire.getQuestions().forEach(q -> q.getOptions().size());

        return toQuestionnaireResponse(questionnaire);
    }

    /**
     * 获取所有活跃问卷
     */
    @Transactional
    public List<QuestionnaireResponse> getActiveQuestionnaires() {
        LocalDateTime now = LocalDateTime.now();
        return questionnaireRepository.findActiveQuestionnaires(now)
                .stream()
                .map(this::toQuestionnaireResponse)
                .collect(Collectors.toList());
    }

    /**
     * 提交答卷
     */
    @Transactional
    public Long submitAnswer(String userId, String userIp, String userAgent, AnswerSubmissionRequest request) {
        Long questionnaireId = request.getQuestionnaireId();
        log.info("提交答卷: userId={}, questionnaireId={}", userId, questionnaireId);

        // 使用分布式锁防止并发提交
        String lockKey = String.format("submit:%s:%s", userId, questionnaireId);
        if (!lockService.tryLock(lockKey)) {
            throw new QuestionnaireException(QuestionnaireException.ErrorCode.QUESTIONNAIRE_LOCKED);
        }

        try {
            return doSubmitAnswer(userId, userIp, userAgent, request);
        } finally {
            lockService.unlock(lockKey);
        }
    }

    /**
     * 执行提交逻辑
     */
    @Transactional
    protected Long doSubmitAnswer(String userId, String userIp, String userAgent, AnswerSubmissionRequest request) {
        Long questionnaireId = request.getQuestionnaireId();

        // 1. 验证问卷
        Questionnaire questionnaire = questionnaireRepository.findByIdBasic(questionnaireId)
                .orElseThrow(() -> new QuestionnaireException(QuestionnaireException.ErrorCode.QUESTIONNAIRE_NOT_FOUND));
        
        // 手动初始化questions集合
        questionnaire.getQuestions().size();
        // 手动初始化每个question的options集合
        questionnaire.getQuestions().forEach(q -> q.getOptions().size());

        if (!questionnaire.getIsActive()) {
            throw new QuestionnaireException(QuestionnaireException.ErrorCode.QUESTIONNAIRE_INACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        if (questionnaire.getStartTime() != null && now.isBefore(questionnaire.getStartTime())) {
            throw new QuestionnaireException(QuestionnaireException.ErrorCode.QUESTIONNAIRE_NOT_STARTED);
        }

        if (questionnaire.getEndTime() != null && now.isAfter(questionnaire.getEndTime())) {
            throw new QuestionnaireException(QuestionnaireException.ErrorCode.QUESTIONNAIRE_EXPIRED);
        }

        // 2. 防重复提交检查
        boolean isAnonymous = request.getIsAnonymous() != null && request.getIsAnonymous();
        if (!isAnonymous) {
            if (answerRepository.existsByUserIdAndQuestionnaireId(userId, questionnaireId)) {
                throw new QuestionnaireException(QuestionnaireException.ErrorCode.DUPLICATE_SUBMISSION);
            }

            // 检查最大提交次数
            if (questionnaire.getMaxSubmissions() != null) {
                Long submissionCount = counterService.getSubmissionCount(userId, questionnaireId);
                if (submissionCount >= questionnaire.getMaxSubmissions()) {
                    throw new QuestionnaireException(QuestionnaireException.ErrorCode.MAX_SUBMISSIONS_REACHED);
                }
            }
        }

        // 3. 创建答卷
        QuestionnaireAnswer answer = QuestionnaireAnswer.builder()
                .questionnaire(questionnaire)
                .userId(isAnonymous ? "anonymous" : userId)
                .userIp(userIp)
                .userAgent(userAgent)
                .isAnonymous(isAnonymous)
                .startTime(request.getStartTime())
                .submittedAt(LocalDateTime.now())
                .build();

        // 计算完成时间
        if (request.getStartTime() != null) {
            answer.setCompletionTime(System.currentTimeMillis() - request.getStartTime());
        }

        // 4. 处理每个题目的答案
        for (AnswerSubmissionRequest.AnswerDTO answerDTO : request.getAnswers()) {
            Question question = questionRepository.findById(answerDTO.getQuestionId())
                    .orElseThrow(() -> new QuestionnaireException(
                            QuestionnaireException.ErrorCode.QUESTION_NOT_FOUND,
                            "题目ID: " + answerDTO.getQuestionId()));

            QuestionAnswer questionAnswer = QuestionAnswer.builder()
                    .question(question)
                    .textAnswer(answerDTO.getTextAnswer())
                    .selectedOptionIds(answerDTO.getSelectedOptionIds() != null ?
                            answerDTO.getSelectedOptionIds().stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(",")) : null)
                    .build();

            answer.addQuestionAnswer(questionAnswer);
        }

        QuestionnaireAnswer savedAnswer = answerRepository.save(answer);

        // 5. 更新计数器
        if (!isAnonymous) {
            counterService.incrementSubmissionCount(userId, questionnaireId, SUBMISSION_COUNTER_TTL);
        }

        // 6. 更新统计
        updateStatistics(questionnaireId, isAnonymous, answer.getCompletionTime());

        // 7. 记录事件
        createEvent("ANSWER_SUBMITTED", questionnaireId, savedAnswer.getId(), userId,
                String.format("提交答卷: questionnaireId=%d", questionnaireId));

        // 8. 创建出站消息
        createOutboxMessage("Answer", savedAnswer.getId(), "ANSWER_SUBMITTED",
                String.format("{\"answerId\":%d,\"questionnaireId\":%d,\"userId\":\"%s\"}",
                        savedAnswer.getId(), questionnaireId, userId));

        log.info("答卷提交成功: answerId={}", savedAnswer.getId());
        return savedAnswer.getId();
    }

    /**
     * 获取问卷统计
     */
    @Transactional
    public StatisticsResponse getStatistics(Long questionnaireId) {
        List<QuestionnaireStatistics> statsList = statisticsRepository
                .findByQuestionnaireIdOrderBySnapshotDateDesc(questionnaireId);

        if (statsList.isEmpty()) {
            // 实时计算统计
            return calculateRealTimeStatistics(questionnaireId);
        }

        QuestionnaireStatistics latest = statsList.get(0);
        return toStatisticsResponse(latest);
    }

    /**
     * 更新统计信息
     */
    @Transactional
    protected void updateStatistics(Long questionnaireId, boolean isAnonymous, Long completionTime) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        QuestionnaireStatistics statistics = statisticsRepository
                .findByQuestionnaireIdAndSnapshotDateBetween(questionnaireId, today, today.plusDays(1))
                .orElseGet(() -> {
                    QuestionnaireStatistics newStats = QuestionnaireStatistics.builder()
                            .questionnaireId(questionnaireId)
                            .snapshotDate(today)
                            .build();
                    return statisticsRepository.save(newStats);
                });

        statistics.setTotalSubmissions(statistics.getTotalSubmissions() + 1);
        statistics.setCompletedSubmissions(statistics.getCompletedSubmissions() + 1);

        if (isAnonymous) {
            statistics.setAnonymousSubmissions(statistics.getAnonymousSubmissions() + 1);
        }

        if (completionTime != null) {
            Long currentCount = statistics.getCompletedSubmissions();
            Double currentAvg = statistics.getAverageCompletionTime() != null ?
                    statistics.getAverageCompletionTime() : 0.0;
            Double newAvg = (currentAvg * (currentCount - 1) + completionTime) / currentCount;
            statistics.setAverageCompletionTime(newAvg);
        }

        statistics.setLastSubmissionAt(LocalDateTime.now());
        statisticsRepository.save(statistics);
    }

    /**
     * 实时计算统计
     */
    private StatisticsResponse calculateRealTimeStatistics(Long questionnaireId) {
        StatisticsResponse response = new StatisticsResponse();
        response.setQuestionnaireId(questionnaireId);
        response.setTotalSubmissions(answerRepository.countByQuestionnaireId(questionnaireId));
        response.setCompletedSubmissions(response.getTotalSubmissions());
        response.setPartialSubmissions(0L);
        response.setUniqueUsers(answerRepository.countUniqueUsersByQuestionnaireId(questionnaireId));
        response.setAnonymousSubmissions(answerRepository.countAnonymousByQuestionnaireId(questionnaireId));
        response.setSnapshotDate(LocalDateTime.now().toString());
        return response;
    }

    /**
     * 创建事件
     */
    private void createEvent(String eventType, Long questionnaireId, Long answerId,
                             String userId, String eventData) {
        QuestionnaireEvent event = QuestionnaireEvent.builder()
                .eventType(eventType)
                .questionnaireId(questionnaireId)
                .answerId(answerId)
                .userId(userId)
                .eventData(eventData)
                .isProcessed(false)
                .build();
        eventRepository.save(event);
    }

    /**
     * 创建出站消息
     */
    private void createOutboxMessage(String aggregateType, Long aggregateId,
                                     String eventType, String eventData) {
        OutboxMessage message = OutboxMessage.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .eventData(eventData)
                .status("PENDING")
                .build();
        outboxMessageRepository.save(message);
    }

    /**
     * 转换为响应DTO
     */
    private QuestionnaireResponse toQuestionnaireResponse(Questionnaire questionnaire) {
        QuestionnaireResponse response = new QuestionnaireResponse();
        response.setId(questionnaire.getId());
        response.setTitle(questionnaire.getTitle());
        response.setDescription(questionnaire.getDescription());
        response.setIsActive(questionnaire.getIsActive());
        response.setMaxSubmissions(questionnaire.getMaxSubmissions());
        response.setAllowAnonymous(questionnaire.getAllowAnonymous());
        response.setStartTime(questionnaire.getStartTime());
        response.setEndTime(questionnaire.getEndTime());
        response.setCreatedBy(questionnaire.getCreatedBy());
        response.setCreatedAt(questionnaire.getCreatedAt());

        if (questionnaire.getQuestions() != null) {
            List<QuestionnaireResponse.QuestionResponse> questionResponses = questionnaire.getQuestions()
                    .stream()
                    .map(this::toQuestionResponse)
                    .collect(Collectors.toList());
            response.setQuestions(questionResponses);
        }

        return response;
    }

    private QuestionnaireResponse.QuestionResponse toQuestionResponse(Question question) {
        QuestionnaireResponse.QuestionResponse response = new QuestionnaireResponse.QuestionResponse();
        response.setId(question.getId());
        response.setContent(question.getContent());
        response.setQuestionType(question.getQuestionType());
        response.setSortOrder(question.getSortOrder());
        response.setIsRequired(question.getIsRequired());

        if (question.getOptions() != null) {
            List<QuestionnaireResponse.OptionResponse> optionResponses = question.getOptions()
                    .stream()
                    .map(option -> {
                        QuestionnaireResponse.OptionResponse optionResponse = new QuestionnaireResponse.OptionResponse();
                        optionResponse.setId(option.getId());
                        optionResponse.setContent(option.getContent());
                        optionResponse.setSortOrder(option.getSortOrder());
                        return optionResponse;
                    })
                    .collect(Collectors.toList());
            response.setOptions(optionResponses);
        }

        return response;
    }

    private StatisticsResponse toStatisticsResponse(QuestionnaireStatistics statistics) {
        StatisticsResponse response = new StatisticsResponse();
        response.setQuestionnaireId(statistics.getQuestionnaireId());
        response.setTotalSubmissions(statistics.getTotalSubmissions());
        response.setCompletedSubmissions(statistics.getCompletedSubmissions());
        response.setPartialSubmissions(statistics.getPartialSubmissions());
        response.setAverageCompletionTime(statistics.getAverageCompletionTime());
        response.setUniqueUsers(statistics.getUniqueUsers());
        response.setAnonymousSubmissions(statistics.getAnonymousSubmissions());
        response.setLastSubmissionAt(statistics.getLastSubmissionAt() != null ?
                statistics.getLastSubmissionAt().toString() : null);
        response.setSnapshotDate(statistics.getSnapshotDate().toString());
        return response;
    }
}
