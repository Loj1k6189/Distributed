package com.example.distributed.lottery.repository;

import com.example.distributed.lottery.domain.LotteryParticipant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LotteryParticipantRepository extends JpaRepository<LotteryParticipant, Long> {

    boolean existsByCampaignIdAndUserId(Long campaignId, String userId);

    Optional<LotteryParticipant> findByCampaignIdAndUserId(Long campaignId, String userId);
}
