package com.example.distributed.vote.repository;

import com.example.distributed.vote.domain.VoteEventEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteEventRepository extends JpaRepository<VoteEventEntity, Long> {

    long countByPollId(Long pollId);

    boolean existsByPollIdAndVoterId(Long pollId, String voterId);

    List<VoteEventEntity> findByPollIdAndCreatedAtAfterOrderByCreatedAtAsc(Long pollId, Instant createdAt);

    @Query("select e.eventId from VoteEventEntity e where e.eventId in :eventIds")
    List<String> findExistingEventIds(@Param("eventIds") List<String> eventIds);
}
