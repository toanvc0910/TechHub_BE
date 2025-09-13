package com.techhub.app.proxyclient.config.cache;//package com.techhub.app.proxyclient.config.cache;
//
//import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
//import org.springframework.cache.annotation.EnableCaching;
//import org.springframework.cache.interceptor.SimpleKeyGenerator;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.cache.RedisCacheConfiguration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
//import org.springframework.data.redis.serializer.RedisSerializationContext;
//import org.springframework.data.redis.serializer.StringRedisSerializer;
//import org.springframework.cache.interceptor.KeyGenerator;
//import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
//
//
//import java.time.Duration;
//
//@Configuration
//@EnableCaching
//@EnableRedisHttpSession
//public class RedisConfig {
//    @Bean
//    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
//        return (builder) -> builder
//                .withCacheConfiguration("userCache", //Thiết lập cấu hình cache với tên là userCache.
//                        RedisCacheConfiguration
//                                .defaultCacheConfig()
//                                .entryTtl(Duration.ofMinutes(20)))
//                .withCacheConfiguration("dataCache", // Thiết lập cấu hình cache với tên là dataCache.
//                        RedisCacheConfiguration
//                                .defaultCacheConfig()
//                                .entryTtl(Duration.ofMinutes(5)));
//    }
//
//    @Bean
//    public RedisCacheConfiguration cacheConfiguration() {
//        return RedisCacheConfiguration
//                .defaultCacheConfig()
//                .entryTtl(Duration.ofMinutes(60))
//                .serializeValuesWith(RedisSerializationContext
//                        .SerializationPair
//                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));
//    }
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(redisConnectionFactory);
////        template.setKeySerializer(new StringRedisSerializer());
////        template.setKeySerializer(new StringRedisSerializer());
////        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
////        template.setValueSerializer(new Jackson2JsonRedisSerializer(Object.class));
//
//        return template;
//    }
//
////    @Bean
////    public static ConfigureRedisAction configureRedisAction() {
////        return ConfigureRedisAction.NO_OP;
////    }
//    @Bean
//    public RedisConnectionFactory redisConnectionFactory() {
//        return new LettuceConnectionFactory();
//    }
////@Bean
////public LettuceConnectionFactory lettuceConnectionFactory() {
////
////    LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
////            .useSsl().and()
////            .commandTimeout(Duration.ofSeconds(2))
////            .shutdownTimeout(Duration.ZERO)
////            .build();
////    RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration();
////    serverConfig.setHostName("172.16.3.4");
////    serverConfig.setPort(6379);
////    serverConfig.setPassword("Oracle@123");
////    return new LettuceConnectionFactory(serverConfig, clientConfig);
////}
//    @Bean
//    public KeyGenerator keyGenerator() {
//        return new SimpleKeyGenerator();
//    }
//}
