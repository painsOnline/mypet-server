/**
 * File: RedisDistributedLock.java
 * Author: system
 * Date: 2026-06-27
 */
package app.xinqianmao.com.admin.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis distributed lock using SETNX + TTL.
 * Lock value is a UUID so only the holder can release.
 */
@Component
@RequiredArgsConstructor
public class RedisDistributedLock {

    private final StringRedisTemplate redisTemplate;

    public boolean acquire(String lockKey, String lockValue, long expireSeconds) {
        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(expireSeconds));
        return Boolean.TRUE.equals(ok);
    }

    public void release(String lockKey, String lockValue) {
        String current = redisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(current)) {
            redisTemplate.delete(lockKey);
        }
    }

    public void extend(String lockKey, String lockValue, long expireSeconds) {
        String current = redisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(current)) {
            redisTemplate.expire(lockKey, Duration.ofSeconds(expireSeconds));
        }
    }
}
