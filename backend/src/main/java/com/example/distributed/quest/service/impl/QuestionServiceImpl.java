package com.example.distributed.quest.service.impl;

import com.example.distributed.quest.api.*;
import com.example.distributed.quest.domain.*;
import com.example.distributed.quest.repository.*;
import com.example.distributed.quest.service.QuestionService;
import com.example.distributed.quest.service.QuestionBusinessException;
import com.example.distributed.quest.service.QuestionOutboxService;
import com.example.distributed.quest.service.RedisQuestionCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {
    
    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionSubmissionRepository questionSubmissionRepository;
    private final SubmissionOptionRepository submissionOptionRepository;
    private final QuestionStatsSnapshotRepository questionStatsSnapshotRepository;
    private final QuestionEventRepository questionEventRepository;
    private final QuestionOutboxService questionOutboxService;
    private final RedisQuestionCounter redisQuestionCounter;
    private final StringRedisTemplate redisTemplate;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    public QuestionCreateResponse createQuestion(QuestionCreateRequest request) {
        // 创建问卷
        Questionnaire questionnaire = new Questionnaire();
        questionnaire.setTitle(request.title());
        questionnaire.setDescription(request.description());
        questionnaire.setAllowMultiple(request.allowMultiple());
        questionnaire.setMaxOptions(request.allowMultiple() ? request.options().size() : 1);
        questionnaire.setStatus(1); // 默认开启
        questionnaire.setCreatorId("system"); // 暂时使用系统默认值
        
        Questionnaire savedQuestionnaire = questionnaireRepository.save(questionnaire);
        
        // 创建选项
        List<Long> optionIds = new ArrayList<>();
        for (int i = 0; i < request.options().size(); i++) {
            String option = request.options().get(i);
            
            QuestionOption questionOption = new QuestionOption();
            questionOption.setQuestionId(savedQuestionnaire.getId());
            questionOption.setOptionType(QuestionOption.OptionType.RADIO); // 默认单选
            questionOption.setOptionKey("option_" + (i + 1));
            questionOption.setOptionValue(option);
            questionOption.setSortOrder(i);
            
            QuestionOption savedOption = questionOptionRepository.save(questionOption);
            optionIds.add(savedOption.getId());
        }
        
        // 初始化Redis计数
        redisQuestionCounter.initializeQuestion(savedQuestionnaire.getId(), optionIds);
        
        return new QuestionCreateResponse(
                savedQuestionnaire.getId(),
                savedQuestionnaire.getTitle(),
                savedQuestionnaire.getDescription(),
                savedQuestionnaire.getAllowMultiple()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionSummaryResponse> getQuestions() {
        List<Questionnaire> questionnaires = questionnaireRepository.findAll();
        
        return questionnaires.stream()
                .map(q -> new QuestionSummaryResponse(
                        q.getId(),
                        q.getTitle(),
                        q.getDescription(),
                        q.getStatus() == 1,
                        q.getCreatedAt(),
                        q.getExpiresAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionDetailResponse getQuestion(Long questionId) {
        Questionnaire questionnaire = questionnaireRepository.findById(questionId)
                .orElseThrow(() -> new QuestionBusinessException("Questionnaire not found"));
        
        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderBySortOrder(questionId);
        
        List<QuestionDetailResponse.OptionInfo> optionInfos = options.stream()
                .map(opt -> new QuestionDetailResponse.OptionInfo(
                        opt.getId(),
                        opt.getOptionKey(),
                        opt.getOptionValue()
                ))
                .collect(Collectors.toList());
        
        return new QuestionDetailResponse(
                questionnaire.getId(),
                questionnaire.getTitle(),
                questionnaire.getDescription(),
                questionnaire.getAllowMultiple(),
                questionnaire.getMaxOptions(),
                questionnaire.getCreatedAt(),
                questionnaire.getExpiresAt(),
                optionInfos
        );
    }

    @Override
    @Transactional
    public QuestionSubmitResponse submitQuestion(QuestionSubmitRequest request, String clientIp) {
        // 检查问卷是否存在
        Questionnaire questionnaire = questionnaireRepository.findById(request.questionId())
                .orElseThrow(() -> new QuestionBusinessException("Questionnaire not found"));
        
        // 检查问卷状态
        if (questionnaire.getStatus() != 1) {
            throw new QuestionBusinessException("Questionnaire is not active");
        }
        
        // 检查是否已提交
        Optional<QuestionSubmission> existingSubmission = 
                questionSubmissionRepository.findByQuestionIdAndUserId(request.questionId(), request.userId());
        if (existingSubmission.isPresent()) {
            throw new QuestionBusinessException("Questionnaire already submitted by user");
        }
        
        // 检查选项数量是否符合要求
        if (!questionnaire.getAllowMultiple() && request.optionIds().size() > 1) {
            throw new QuestionBusinessException("This questionnaire only allows single selection");
        }
        
        if (request.optionIds().size() > questionnaire.getMaxOptions()) {
            throw new QuestionBusinessException("Too many options selected");
        }
        
        // 使用Redis实现分布式锁
        String lockKey = "question_lock:" + request.questionId();
        String lockValue = UUID.randomUUID().toString();
        
        try {
            // 尝试获取锁，使用SET命令的NX和EX参数实现原子性
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);
            
            if (!acquired) {
                throw new QuestionBusinessException("Another operation is in progress, please try again later.");
            }
            
            try {
                // 原子性增加Redis计数
                redisQuestionCounter.incrementQuestion(request.questionId(), request.optionIds());
                
                // 创建提交记录
                QuestionSubmission submission = new QuestionSubmission();
                submission.setQuestionId(request.questionId());
                submission.setUserId(request.userId());
                submission.setClientIp(clientIp);
                
                QuestionSubmission savedSubmission = questionSubmissionRepository.save(submission);
                
                // 创建提交选项记录
                for (Long optionId : request.optionIds()) {
                    SubmissionOption submissionOption = new SubmissionOption();
                    submissionOption.setSubmissionId(savedSubmission.getId());
                    submissionOption.setOptionId(optionId);
                    
                    submissionOptionRepository.save(submissionOption);
                }
                
                // 更新统计快照
                updateStatsSnapshots(request.questionId(), request.optionIds());
                
                // 创建事件并发送MQ消息
                QuestionEventMessage eventMessage = new QuestionEventMessage(
                        UUID.randomUUID().toString(),
                        request.questionId(),
                        request.userId(),
                        clientIp,
                        request.optionIds(),
                        Instant.now()
                );
                
                try {
                    QuestionOutboxMessage outboxMessage = questionOutboxService.saveOutbox(eventMessage);
                    questionOutboxService.tryPublish(outboxMessage);
                } catch (RuntimeException ex) {
                    // 如果发送事件失败，回滚Redis计数
                    redisQuestionCounter.rollbackIncrement(request.questionId(), request.optionIds());
                    throw new QuestionBusinessException("EVENT_WRITE_FAILED", "事务消息保存失败，问卷已回滚");
                }
                
                return new QuestionSubmitResponse(
                        savedSubmission.getId().toString(),
                        request.questionId(),
                        savedSubmission.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant()
                );
            } finally {
                // 释放锁，使用Lua脚本确保只有当前线程能删除自己的锁
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) else return 0 end";
                redisTemplate.execute(new DefaultRedisScript<>(luaScript, Long.class), 
                        Collections.singletonList(lockKey), lockValue);
            }
        } catch (Exception e) {
            if (e instanceof QuestionBusinessException) {
                throw e; // 重新抛出业务异常
            }
            throw new QuestionBusinessException("Failed to acquire distributed lock", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionSubmittedResponse checkSubmission(String userId, Long questionId) {
        Optional<QuestionSubmission> submission = 
                questionSubmissionRepository.findByQuestionIdAndUserId(questionId, userId);
        return new QuestionSubmittedResponse(submission.isPresent());
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionStatsResponse getQuestionStats(Long questionId) {
        // 获取问卷信息
        Questionnaire questionnaire = questionnaireRepository.findById(questionId)
                .orElseThrow(() -> new QuestionBusinessException("Questionnaire not found"));
        
        // 获取所有选项
        List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderBySortOrder(questionId);
        List<Long> optionIds = options.stream().map(QuestionOption::getId).collect(Collectors.toList());
        
        // 从Redis获取实时统计数据
        Map<Long, Long> countByOption = redisQuestionCounter.pollOptionCounts(questionId, optionIds);
        long totalVotes = redisQuestionCounter.pollBallots(questionId);
        
        // 如果Redis中没有数据，则从数据库获取
        if (countByOption.values().stream().allMatch(value -> value == 0L) && totalVotes == 0L) {
            List<QuestionSubmission> submissions = questionSubmissionRepository.findAll();
            List<QuestionSubmission> questionSubmissions = submissions.stream()
                    .filter(sub -> sub.getQuestionId().equals(questionId))
                    .collect(Collectors.toList());
            
            // 获取所有提交选项
            List<Long> submissionIds = questionSubmissions.stream()
                    .map(QuestionSubmission::getId)
                    .collect(Collectors.toList());
            
            List<SubmissionOption> submissionOptions = submissionOptionRepository.findAll();
            if (!submissionIds.isEmpty()) {
                submissionOptions = submissionOptions.stream()
                        .filter(so -> submissionIds.contains(so.getSubmissionId()))
                        .collect(Collectors.toList());
            }
            
            // 计算各选项票数
            countByOption = submissionOptions.stream()
                    .collect(Collectors.groupingBy(
                            SubmissionOption::getOptionId,
                            Collectors.counting()
                    ));
            
            totalVotes = submissionOptions.size();
        }
        
        // 获取选项详细信息
        Map<Long, String> optionValues = options.stream()
                .collect(Collectors.toMap(
                        QuestionOption::getId,
                        QuestionOption::getOptionValue
                ));
        
        // 构造选项统计信息
        List<QuestionStatsResponse.OptionStats> optionStatsList = new ArrayList<>();
        
        for (int i = 0; i < options.size(); i++) {
            QuestionOption option = options.get(i);
            Long votes = countByOption.getOrDefault(option.getId(), 0L);
            Double percentage = totalVotes > 0 ? (double) votes / totalVotes * 100 : 0.0;
            
            optionStatsList.add(new QuestionStatsResponse.OptionStats(
                    option.getId(),
                    option.getOptionValue(),
                    votes,
                    percentage,
                    i + 1 // 简单排名
            ));
        }
        
        // 按票数降序排列
        optionStatsList.sort((o1, o2) -> o2.votes().compareTo(o1.votes()));
        
        // 重新分配排名
        for (int i = 0; i < optionStatsList.size(); i++) {
            QuestionStatsResponse.OptionStats oldStat = optionStatsList.get(i);
            optionStatsList.set(i, new QuestionStatsResponse.OptionStats(
                    oldStat.optionId(),
                    oldStat.optionText(),
                    oldStat.votes(),
                    oldStat.percentage(),
                    i + 1
            ));
        }
        
        return new QuestionStatsResponse(
                questionId,
                questionnaire.getTitle(),
                totalVotes,
                optionStatsList,
                Instant.now()
        );
    }
    
    private void updateStatsSnapshots(Long questionId, List<Long> optionIds) {
        for (Long optionId : optionIds) {
            // 这里简化处理，实际应用中可能需要更复杂的统计逻辑
            List<QuestionStatsSnapshot> snapshots = questionStatsSnapshotRepository.findByQuestionId(questionId);
            Optional<QuestionStatsSnapshot> existingSnapshot = snapshots.stream()
                    .filter(s -> s.getOptionId().equals(optionId))
                    .findFirst();
            
            if (existingSnapshot.isPresent()) {
                QuestionStatsSnapshot snapshot = existingSnapshot.get();
                snapshot.setVoteCount(snapshot.getVoteCount() + 1);
                questionStatsSnapshotRepository.save(snapshot);
            } else {
                QuestionStatsSnapshot newSnapshot = new QuestionStatsSnapshot();
                newSnapshot.setQuestionId(questionId);
                newSnapshot.setOptionId(optionId);
                newSnapshot.setVoteCount(1L);
                newSnapshot.setSnapshotDate(java.time.LocalDate.now());
                questionStatsSnapshotRepository.save(newSnapshot);
            }
        }
    }
}