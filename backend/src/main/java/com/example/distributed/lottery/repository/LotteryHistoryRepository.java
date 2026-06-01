package com.example.distributed.lottery.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.distributed.lottery.domain.LotteryHistory;

public interface LotteryHistoryRepository extends JpaRepository<LotteryHistory, Long> {
    Page<LotteryHistory> findByActivityId(String activityId, Pageable pageable);
    List<LotteryHistory> findTop50ByActivityIdOrderByWonAtDesc(String activityId);

    @Query("select distinct l.activityId from LotteryHistory l")
    List<String> findDistinctActivityIds();
}