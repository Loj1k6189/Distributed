package com.example.distributed.chain.service;

import com.example.distributed.chain.dto.ChainCreateRequest;
import com.example.distributed.chain.dto.ChainEntryRequest;
import com.example.distributed.chain.dto.ChainEntryResponse;
import com.example.distributed.chain.dto.ChainResponse;
import com.example.distributed.chain.entity.Chain;
import com.example.distributed.chain.entity.ChainEntry;
import com.example.distributed.chain.entity.ChainEvent;
import com.example.distributed.chain.exception.ChainException;
import com.example.distributed.chain.repository.ChainEntryRepository;
import com.example.distributed.chain.repository.ChainEventRepository;
import com.example.distributed.chain.repository.ChainRepository;
import com.example.distributed.quest.service.DistributedLockService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 接龙核心业务服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChainService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Shanghai");

    private final ChainRepository chainRepository;
    private final ChainEntryRepository chainEntryRepository;
    private final ChainEventRepository chainEventRepository;
    private final DistributedLockService lockService;

    /**
     * 创建接龙
     */
    @Transactional
    @CacheEvict(value = "chains", allEntries = true)
    public Chain createChain(String userId, ChainCreateRequest request) {
        log.info("创建接龙: userId={}, title={}", userId, request.getTitle());

        // 检查是否已存在同名接龙
        if (chainRepository.existsByCreatedByAndTitle(userId, request.getTitle())) {
            throw new ChainException(ChainException.ErrorCode.DUPLICATE_ENTRY, "您已创建过同名接龙");
        }

        Chain chain = Chain.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .createdBy(userId)
                .isActive(true)
                .allowMultiple(request.getAllowMultiple() != null ? request.getAllowMultiple() : false)
                .maxParticipants(request.getMaxParticipants())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        Chain saved = chainRepository.save(chain);

        // 记录事件
        createEvent("CHAIN_CREATED", saved.getId(), null, userId,
                String.format("创建接龙: %s", saved.getTitle()));

        log.info("接龙创建成功: id={}", saved.getId());
        return saved;
    }

    /**
     * 获取接龙详情（带缓存）
     */
    @Cacheable(value = "chains", key = "#id")
    @Transactional
    public ChainResponse getChain(Long id) {
        Chain chain = chainRepository.findByIdBasic(id)
                .orElseThrow(() -> new ChainException(ChainException.ErrorCode.CHAIN_NOT_FOUND));

        // 手动初始化entries集合
        List<ChainEntry> entries = chainEntryRepository.findByChainIdOrderBySequenceNoAsc(id);

        return toChainResponse(chain, entries);
    }

    /**
     * 获取所有活跃接龙
     */
    @Transactional
    public List<ChainResponse> getActiveChains() {
        LocalDateTime now = currentTime();
        return chainRepository.findByIsActiveTrue()
                .stream()
                .filter(chain -> isActiveAt(chain, now))
                .map(chain -> {
                    List<ChainEntry> entries = chainEntryRepository.findByChainIdOrderBySequenceNoAsc(chain.getId());
                    return toChainResponse(chain, entries);
                })
                .collect(Collectors.toList());
    }

    /**
     * 参与接龙（核心方法 - 使用分布式锁保证顺序一致性）
     */
    @Transactional
    @CacheEvict(value = "chains", key = "#chainId")
    public Long joinChain(String userId, Long chainId, ChainEntryRequest request) {
        log.info("参与接龙: userId={}, chainId={}", userId, chainId);

        // 使用分布式锁防止并发追加冲突
        String lockKey = String.format("chain:%s:join", chainId);
        if (!lockService.tryLock(lockKey)) {
            throw new ChainException(ChainException.ErrorCode.CHAIN_LOCKED);
        }

        try {
            return doJoinChain(userId, chainId, request);
        } finally {
            lockService.unlock(lockKey);
        }
    }

    /**
     * 执行接龙参与逻辑
     */
    protected Long doJoinChain(String userId, Long chainId, ChainEntryRequest request) {
        // 1. 验证接龙
        Chain chain = chainRepository.findByIdBasic(chainId)
                .orElseThrow(() -> new ChainException(ChainException.ErrorCode.CHAIN_NOT_FOUND));

        if (!chain.getIsActive()) {
            throw new ChainException(ChainException.ErrorCode.CHAIN_INACTIVE);
        }

        LocalDateTime now = currentTime();
        if (chain.getStartTime() != null && now.isBefore(chain.getStartTime())) {
            throw new ChainException(ChainException.ErrorCode.CHAIN_NOT_STARTED);
        }
        if (chain.getEndTime() != null && now.isAfter(chain.getEndTime())) {
            throw new ChainException(ChainException.ErrorCode.CHAIN_EXPIRED);
        }

        // 2. 检查人数限制
        Long currentCount = chainEntryRepository.countByChainId(chainId);
        if (chain.getMaxParticipants() != null && currentCount >= chain.getMaxParticipants()) {
            throw new ChainException(ChainException.ErrorCode.CHAIN_FULL);
        }

        // 3. 检查是否已参与
        boolean alreadyJoined = chainEntryRepository.existsByChainIdAndUserId(chainId, userId);
        if (alreadyJoined && !chain.getAllowMultiple()) {
            throw new ChainException(ChainException.ErrorCode.MULTIPLE_NOT_ALLOWED);
        }

        // 4. 生成序号（获取当前最大序号 + 1）
        Long maxSequenceNo = chainEntryRepository.findMaxSequenceNoByChainId(chainId);
        Long newSequenceNo = maxSequenceNo + 1;

        // 5. 创建接龙项
        ChainEntry entry = ChainEntry.builder()
                .chain(chain)
                .userId(userId)
                .content(request.getContent())
                .sequenceNo(newSequenceNo)
                .parentEntryId(request.getParentEntryId())
                .build();

        ChainEntry savedEntry = chainEntryRepository.save(entry);

        // 6. 记录事件
        createEvent("CHAIN_JOINED", chainId, savedEntry.getId(), userId,
                String.format("用户 %s 参与接龙，序号: %d", userId, newSequenceNo));

        log.info("接龙参与成功: chainId={}, entryId={}, sequenceNo={}", chainId, savedEntry.getId(), newSequenceNo);
        return savedEntry.getId();
    }

    /**
     * 删除接龙
     */
    @Transactional
    @CacheEvict(value = "chains", allEntries = true)
    public void deleteChain(String userId, Long chainId) {
        log.info("删除接龙: userId={}, chainId={}", userId, chainId);

        Chain chain = chainRepository.findByIdBasic(chainId)
                .orElseThrow(() -> new ChainException(ChainException.ErrorCode.CHAIN_NOT_FOUND));

        if (!chain.getCreatedBy().equals(userId)) {
            throw new ChainException(ChainException.ErrorCode.CHAIN_NOT_FOUND, "您无权删除此接龙");
        }

        chain.setIsActive(false);
        chainRepository.save(chain);

        createEvent("CHAIN_DELETED", chainId, null, userId, "删除接龙");
        log.info("接龙删除成功: chainId={}", chainId);
    }

    /**
     * 创建事件记录
     */
    private void createEvent(String eventType, Long chainId, Long entryId, String userId, String eventData) {
        ChainEvent event = ChainEvent.builder()
                .eventType(eventType)
                .chainId(chainId)
                .entryId(entryId)
                .userId(userId)
                .eventData(eventData)
                .isProcessed(false)
                .build();

        chainEventRepository.save(event);
    }

    /**
     * 转换为ChainResponse
     */
    private ChainResponse toChainResponse(Chain chain, List<ChainEntry> entries) {
        List<ChainEntryResponse> entryResponses = entries.stream()
                .map(this::toEntryResponse)
                .collect(Collectors.toList());

        return ChainResponse.builder()
                .id(chain.getId())
                .title(chain.getTitle())
                .description(chain.getDescription())
                .createdBy(chain.getCreatedBy())
                .maxParticipants(chain.getMaxParticipants())
                .allowMultiple(chain.getAllowMultiple())
                .isActive(chain.getIsActive())
                .startTime(chain.getStartTime())
                .endTime(chain.getEndTime())
                .createdAt(chain.getCreatedAt())
                .participantCount((long) entries.size())
                .entries(entryResponses)
                .build();
    }

    /**
     * 转换为ChainEntryResponse
     */
    private ChainEntryResponse toEntryResponse(ChainEntry entry) {
        return ChainEntryResponse.builder()
                .id(entry.getId())
                .chainId(entry.getChain().getId())
                .userId(entry.getUserId())
                .content(entry.getContent())
                .sequenceNo(entry.getSequenceNo())
                .parentEntryId(entry.getParentEntryId())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private boolean isActiveAt(Chain chain, LocalDateTime now) {
        if (chain.getStartTime() != null && chain.getStartTime().isAfter(now)) {
            return false;
        }
        return chain.getEndTime() == null || !chain.getEndTime().isBefore(now);
    }

    private LocalDateTime currentTime() {
        return LocalDateTime.now(APP_ZONE);
    }
}
