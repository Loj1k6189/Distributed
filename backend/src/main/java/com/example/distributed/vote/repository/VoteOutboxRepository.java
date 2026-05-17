package com.example.distributed.vote.repository;

import com.example.distributed.vote.domain.VoteOutboxMessage;
import com.example.distributed.vote.domain.VoteOutboxStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteOutboxRepository extends JpaRepository<VoteOutboxMessage, Long> {

    @Query("""
            select o
            from VoteOutboxMessage o
            where o.status <> :sentStatus
              and (o.nextRetryAt is null or o.nextRetryAt <= :now)
            order by o.createdAt asc
            """)
    List<VoteOutboxMessage> findRetryable(@Param("sentStatus") VoteOutboxStatus sentStatus,
                                          @Param("now") Instant now,
                                          Pageable pageable);
}
