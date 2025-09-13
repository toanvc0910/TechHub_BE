package com.techhub.app.proxyclient.business.user.service;//package com.techhub.app.proxyclient.business.user.service;
//
//import lombok.AllArgsConstructor;
//import org.springframework.cache.Cache;
//import org.springframework.cache.CacheManager;
//import org.springframework.data.redis.core.RedisCallback;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Service;
//
//import java.util.concurrent.TimeUnit;
//
//@Service
//@AllArgsConstructor
//public class CacheService {
//    private final  RedisTemplate<String, Object> redisTemplate;
//
//    private final CacheManager cacheManager;
//    public void setCacheValue(String key, Object value) {
//        redisTemplate.opsForValue().set(key, value);
//    }
//
//    public void setCacheValueWithTTL(String key, Object value, long timeout, TimeUnit unit) {
//        redisTemplate.opsForValue().set(key, value, timeout, unit);
//    }
//
//    public Object getCacheValue(String key) {
//        Object value = redisTemplate.opsForValue().get(key);
//        return value;
//    }
//
//    public void deleteCacheValue(String key) {
//        redisTemplate.delete(key);
//    }
//
//    public void clearAllCaches() {
////        cacheManager.getCacheNames().forEach(cacheName -> cacheManager.getCache(cacheName).clear());
//        redisTemplate.getConnectionFactory().getConnection().flushDb();
//    }
//
//    public Object getVendorFromCache(String value,String vendorId) {
//        Cache cache = cacheManager.getCache(value);
//        if (cache != null) {
//            return cache.get(vendorId).get();
//        }
//        return null;
//    }
//
//    public String ping() {
//        return redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
//    }
//}
