package com.example.distributed.vote.repository;

import com.example.distributed.vote.domain.VoteOptionCount;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VoteOptionCountRepository extends JpaRepository<VoteOptionCount, Long> {

    List<VoteOptionCount> findByPollId(Long pollId);

    @Modifying
    @Query(value = """
            INSERT INTO vote_option_count(poll_id, option_id, vote_count, updated_at)
            VALUES (:pollId, :optionId, :increment, :updatedAt)
            ON DUPLICATE KEY UPDATE
            vote_count = vote_count + VALUES(vote_count),
            updated_at = VALUES(updated_at)
            """, nativeQuery = true)
    void upsertCount(@Param("pollId") Long pollId,
                     @Param("optionId") Long optionId,
                     @Param("increment") Long increment,
                     @Param("updatedAt") Instant updatedAt);
}

