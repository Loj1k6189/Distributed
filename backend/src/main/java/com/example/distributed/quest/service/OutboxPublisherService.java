package com.example.distributed.quest.service;

import com.example.distributed.quest.entity.OutboxMessage;
import com.example.distributed.quest.repository.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox消息发布服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {

    private final OutboxMessageRepository outboxMessageRepository;

    /**
     * 定时发布待发送的消息
     */
    @Scheduled(fixedDelay = 3000) // 每3秒执行一次
    @Transactional
    public void publishPendingMessages() {
        List<OutboxMessage> pendingMessages = outboxMessageRepository.findRetryableMessages();

        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("开始发布Outbox消息，数量: {}", pendingMessages.size());

        for (OutboxMessage message : pendingMessages) {
            try {
                publishMessage(message);
                
                message.setStatus("PUBLISHED");
                message.setPublishedAt(LocalDateTime.now());
                outboxMessageRepository.save(message);
                
                log.debug("消息发布成功: messageId={}, type={}", 
                        message.getId(), message.getEventType());
            } catch (Exception e) {
                log.error("消息发布失败: messageId={}, type={}, error={}", 
                        message.getId(), message.getEventType(), e.getMessage());
                
                message.setRetryCount(message.getRetryCount() + 1);
                message.setErrorMessage(e.getMessage());
                
                // 计算下次重试时间（指数退避）
                long delayMillis = (long) (1000 * Math.pow(2, message.getRetryCount()));
                message.setNextRetryAt(LocalDateTime.now().plusNanos(delayMillis * 1_000_000));
                
                if (message.getRetryCount() >= message.getMaxRetries()) {
                    message.setStatus("FAILED");
                    log.error("消息发布失败且达到最大重试次数: messageId={}", message.getId());
                }
                
                outboxMessageRepository.save(message);
            }
        }
    }

    /**
     * 发布单条消息
     */
    private void publishMessage(OutboxMessage message) {
        log.info("发布消息: type={}, aggregateType={}, aggregateId={}", 
                message.getEventType(), message.getAggregateType(), message.getAggregateId());
        
        // 这里可以集成真实的消息队列（如RabbitMQ、Kafka等）
        // 示例代码：
        // rabbitTemplate.convertAndSend("exchange", "routingKey", message.getEventData());
        
        // 模拟发布成功
        try {
            Thread.sleep(10); // 模拟网络延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
