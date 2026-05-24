package com.example.distributed.quest.service;

import com.example.distributed.quest.entity.QuestionnaireEvent;
import com.example.distributed.quest.repository.QuestionnaireEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 问卷事件处理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionnaireEventService {

    private final QuestionnaireEventRepository eventRepository;

    /**
     * 定时处理未处理的事件
     */
    @Scheduled(fixedDelay = 5000) // 每5秒执行一次
    @Transactional
    public void processPendingEvents() {
        List<QuestionnaireEvent> pendingEvents = eventRepository.findByIsProcessedFalseOrderByCreatedAtAsc();

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("开始处理待处理事件，数量: {}", pendingEvents.size());

        for (QuestionnaireEvent event : pendingEvents) {
            try {
                processEvent(event);
                
                event.setIsProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                eventRepository.save(event);
                
                log.debug("事件处理成功: eventId={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("事件处理失败: eventId={}, type={}, error={}", 
                        event.getId(), event.getEventType(), e.getMessage());
                
                event.setRetryCount(event.getRetryCount() + 1);
                eventRepository.save(event);
                
                if (event.getRetryCount() >= 3) {
                    log.warn("事件重试次数已达上限，标记为失败: eventId={}", event.getId());
                    // 可以添加失败处理逻辑，如发送告警
                }
            }
        }
    }

    /**
     * 处理单个事件
     */
    private void processEvent(QuestionnaireEvent event) {
        switch (event.getEventType()) {
            case "QUESTIONNAIRE_CREATED":
                handleQuestionnaireCreated(event);
                break;
            case "ANSWER_SUBMITTED":
                handleAnswerSubmitted(event);
                break;
            default:
                log.warn("未知的事件类型: {}", event.getEventType());
        }
    }

    /**
     * 处理问卷创建事件
     */
    private void handleQuestionnaireCreated(QuestionnaireEvent event) {
        log.info("处理问卷创建事件: questionnaireId={}", event.getQuestionnaireId());
        // 可以触发邮件通知、缓存预热等操作
    }

    /**
     * 处理答卷提交事件
     */
    private void handleAnswerSubmitted(QuestionnaireEvent event) {
        log.info("处理答卷提交事件: answerId={}", event.getAnswerId());
        // 可以触发邮件通知、数据统计更新等操作
    }
}
