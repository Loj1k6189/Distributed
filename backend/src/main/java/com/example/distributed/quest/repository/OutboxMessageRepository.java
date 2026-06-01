package com.example.distributed.quest.repository;

import com.example.distributed.quest.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 出站消息Repository
 */
@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    @Query("SELECT o FROM OutboxMessage o WHERE o.status = 'PENDING' AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now)")
    List<OutboxMessage> findPendingMessages(@Param("now") LocalDateTime now);

    List<OutboxMessage> findByStatusAndEventType(String status, String eventType);

    @Query("SELECT o FROM OutboxMessage o WHERE o.status = 'PENDING' AND o.retryCount < o.maxRetries ORDER BY o.createdAt ASC")
    List<OutboxMessage> findRetryableMessages();
}
