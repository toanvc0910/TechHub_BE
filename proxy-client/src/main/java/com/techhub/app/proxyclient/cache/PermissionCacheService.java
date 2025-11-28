package com.techhub.app.proxyclient.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Cache service for storing and retrieving permission check results
 * Cache key format: "permission:{userId}:{url}:{method}"
 * TTL: 5 minutes
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "permission:";
    private static final long CACHE_TTL_MINUTES = 5;

    /**
     * Generate cache key for permission check
     */
    private String generateKey(UUID userId, String url, String method) {
        return CACHE_PREFIX + userId + ":" + url + ":" + method;
    }

    /**
     * Get cached permission result
     * 
     * @return Boolean if cached, null if not found
     */
    public Boolean getPermission(UUID userId, String url, String method) {
        try {
            String key = generateKey(userId, url, method);
            log.info("🔑 [PermissionCacheService] Generated cache key: {}", key);

            Object value = redisTemplate.opsForValue().get(key);
            log.info("📦 [PermissionCacheService] Redis get result: {}", value);

            if (value instanceof Boolean) {
                log.info("✅ [PermissionCacheService] Cache HIT for permission: {} = {}", key, value);
                return (Boolean) value;
            }

            log.info("❌ [PermissionCacheService] Cache MISS for permission: {}", key);
            return null;
        } catch (Exception e) {
            log.error("❌ [PermissionCacheService] Redis error getting permission for user {} on {} {} - Error: {}",
                    userId, method, url, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Cache permission result with 5 minutes TTL
     */
    public void cachePermission(UUID userId, String url, String method, boolean allowed) {
        try {
            String key = generateKey(userId, url, method);
            log.info("💾 [PermissionCacheService] Caching permission: {} = {} (TTL: {} min)", key, allowed,
                    CACHE_TTL_MINUTES);

            redisTemplate.opsForValue().set(key, allowed, CACHE_TTL_MINUTES, TimeUnit.MINUTES);

            log.info("✅ [PermissionCacheService] Cached successfully: {}", key);
        } catch (Exception e) {
            log.error("❌ [PermissionCacheService] Redis error caching permission for user {} on {} {} - Error: {}",
                    userId, method, url, e.getMessage(), e);
        }
    }

    /**
     * Clear all cached permissions for a specific user
     * Called when user's roles or permissions are updated
     */
    public void clearUserPermissions(UUID userId) {
        try {
            String pattern = CACHE_PREFIX + userId + ":*";
            var keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cached permissions for user {}", keys.size(), userId);
            }
        } catch (Exception e) {
            log.error("Error clearing permissions cache for user {}", userId, e);
        }
    }

    /**
     * Clear all cached permissions
     * Called when role or permission definitions are updated
     */
    public void clearAllPermissions() {
        try {
            String pattern = CACHE_PREFIX + "*";
            var keys = redisTemplate.keys(pattern);

            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared all {} cached permissions", keys.size());
            }
        } catch (Exception e) {
            log.error("Error clearing all permissions cache", e);
        }
    }
}
