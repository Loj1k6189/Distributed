package com.example.distributed.lottery.repository;

import com.example.distributed.lottery.domain.LotteryWinner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryWinnerRepository extends JpaRepository<LotteryWinner, Long> {

    List<LotteryWinner> findByCampaignIdOrderByRoundNoAsc(Long campaignId);
}
