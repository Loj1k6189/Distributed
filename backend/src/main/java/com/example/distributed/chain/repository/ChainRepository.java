package com.example.distributed.chain.repository;

import com.example.distributed.chain.entity.Chain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 接龙Repository
 */
@Repository
public interface ChainRepository extends JpaRepository<Chain, Long> {

    @Query("SELECT c FROM Chain c WHERE c.id = :id")
    Optional<Chain> findByIdBasic(@Param("id") Long id);

    @Query("SELECT c FROM Chain c LEFT JOIN c.entries e WHERE c.id = :id")
    Optional<Chain> findByIdWithEntries(@Param("id") Long id);

    List<Chain> findByIsActiveTrue();

    List<Chain> findByIsActiveTrueAndCreatedBy(String createdBy);

    @Query("SELECT c FROM Chain c WHERE c.isActive = true AND c.startTime <= :now AND (c.endTime IS NULL OR c.endTime >= :now)")
    List<Chain> findActiveChains(@Param("now") LocalDateTime now);

    boolean existsByCreatedByAndTitle(String createdBy, String title);
}
