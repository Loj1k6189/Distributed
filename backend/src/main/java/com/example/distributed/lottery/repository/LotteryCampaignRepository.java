package com.example.distributed.lottery.repository;

import com.example.distributed.lottery.domain.LotteryCampaign;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LotteryCampaignRepository extends JpaRepository<LotteryCampaign, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LotteryCampaign c where c.id = :id")
    Optional<LotteryCampaign> findByIdForUpdate(@Param("id") Long id);
}
