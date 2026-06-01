package com.example.distributed.quest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedLockService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取分布式锁（非阻塞）
     * 
     * @param lockKey 锁的key
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, 10);
    }

    /**
     * 尝试获取分布式锁
     * 
     * @param lockKey 锁的key
     * @param timeoutSeconds 锁过期时间（秒）
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, int timeoutSeconds) {
        try {
            Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(
                    lockKey, 
                    String.valueOf(System.currentTimeMillis()), 
                    timeoutSeconds, 
                    TimeUnit.SECONDS
            );
            
            if (success != null && success) {
                log.debug("获取分布式锁成功: key={}", lockKey);
                return true;
            }
            
            log.debug("获取分布式锁失败: key={}", lockKey);
            return false;
        } catch (Exception e) {
            log.error("获取分布式锁异常: key={}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放分布式锁
     * 
     * @param lockKey 锁的key
     */
    public void unlock(String lockKey) {
        try {
            Boolean result = stringRedisTemplate.delete(lockKey);
            if (result != null && result) {
                log.debug("释放分布式锁成功: key={}", lockKey);
            }
        } catch (Exception e) {
            log.error("释放分布式锁异常: key={}", lockKey, e);
        }
    }

    /**
     * 检查锁是否存在
     * 
     * @param lockKey 锁的key
     * @return 是否存在
     */
    public boolean isLocked(String lockKey) {
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey));
        } catch (Exception e) {
            log.error("检查锁状态异常: key={}", lockKey, e);
            return false;
        }
    }
}
