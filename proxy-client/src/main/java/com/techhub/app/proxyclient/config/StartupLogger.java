package com.techhub.app.proxyclient.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupLogger {

    private final RedisTemplate<String, Object> redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void logStartup() {
        log.info("================================================");
        log.info("🚀 PROXY-CLIENT APPLICATION STARTED");
        log.info("================================================");

        // Test Redis connection
        try {
            log.info("🔍 Testing Redis connection...");
            redisTemplate.opsForValue().set("proxy-client:health-check", "OK");
            String result = (String) redisTemplate.opsForValue().get("proxy-client:health-check");

            if ("OK".equals(result)) {
                log.info("✅ Redis connection: SUCCESS");
                log.info("📦 Redis ready for permission caching");
            } else {
                log.error("❌ Redis connection: FAILED - unexpected result: {}", result);
            }

            redisTemplate.delete("proxy-client:health-check");
        } catch (Exception e) {
            log.error("❌ Redis connection: FAILED - Error: {}", e.getMessage(), e);
            log.error("⚠️ Permission caching will NOT work without Redis!");
        }

        log.info("================================================");
        log.info("📋 Features enabled:");
        log.info("   - JWT Authentication Filter");
        log.info("   - Permission-based Authorization");
        log.info("   - Redis Permission Cache (5 min TTL)");
        log.info("   - Kafka Cache Invalidation Consumer");
        log.info("================================================");
    }
}
