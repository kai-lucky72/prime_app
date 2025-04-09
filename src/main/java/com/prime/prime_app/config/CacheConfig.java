package com.prime.prime_app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.redis.time-to-live:3600000}")
    private long timeToLive;

    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMillis(timeToLive))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Custom cache configurations for different cache names
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // User cache with 1 hour TTL
        cacheConfigurations.put("userCache", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)));
        
        // Client cache with 30 minutes TTL
        cacheConfigurations.put("clientCache", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)));
        
        // Performance metrics cache with 15 minutes TTL
        cacheConfigurations.put("performanceCache", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)));
        
        // Attendance records cache with 1 hour TTL
        cacheConfigurations.put("attendanceCache", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)));

        // Rate limiting cache with 1 minute TTL
        cacheConfigurations.put("rateLimitCache", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(1)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
    
    @Primary
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false", matchIfMissing = true)
    public CacheManager simpleCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(Arrays.asList(
            "userCache", 
            "clientCache", 
            "performanceCache", 
            "attendanceCache", 
            "rateLimitCache"
        ));
        return cacheManager;
    }
    
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "false", matchIfMissing = true)
    public RedisTemplate<String, String> redisStringTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }
}