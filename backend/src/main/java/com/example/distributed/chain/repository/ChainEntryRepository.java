package com.example.distributed.chain.repository;

import com.example.distributed.chain.entity.ChainEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 接龙项Repository
 */
@Repository
public interface ChainEntryRepository extends JpaRepository<ChainEntry, Long> {

    List<ChainEntry> findByChainIdOrderBySequenceNoAsc(Long chainId);

    Optional<ChainEntry> findByChainIdAndUserId(Long chainId, String userId);

    boolean existsByChainIdAndUserId(Long chainId, String userId);

    @Query("SELECT COALESCE(MAX(e.sequenceNo), 0) FROM ChainEntry e WHERE e.chain.id = :chainId")
    Long findMaxSequenceNoByChainId(@Param("chainId") Long chainId);

    Long countByChainId(Long chainId);

    @Query("SELECT e FROM ChainEntry e WHERE e.chain.id = :chainId AND e.parentEntryId = :parentEntryId ORDER BY e.createdAt ASC")
    List<ChainEntry> findByChainIdAndParentEntryId(@Param("chainId") Long chainId, @Param("parentEntryId") Long parentEntryId);
}
