package com.example.distributed.chain.repository;

import com.example.distributed.chain.entity.ChainEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 接龙事件Repository
 */
@Repository
public interface ChainEventRepository extends JpaRepository<ChainEvent, Long> {

    List<ChainEvent> findByIsProcessedFalseOrderByCreatedAtAsc();

    List<ChainEvent> findByEventTypeAndIsProcessedFalse(String eventType);
}
