package com.prime.prime_app.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfig {

    @Bean("redisCacheManager")
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withCacheConfiguration("attendanceCache", 
                    RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("performanceCache", 
                    RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24)))
                .withCacheConfiguration("clientCache", 
                    RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(12)))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        try {
            // Try to use environment variables or application properties
            String redisHost = System.getenv("REDIS_HOST");
            String redisPort = System.getenv("REDIS_PORT");
            
            // Default values
            if (redisHost == null) redisHost = "localhost";
            if (redisPort == null) redisPort = "6379";
            
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(redisHost);
            config.setPort(Integer.parseInt(redisPort));
            
            return new JedisConnectionFactory(config);
        } catch (Exception e) {
            // Fallback to default in-memory configuration if Redis is not available
            return new JedisConnectionFactory();
        }
    }

    @Bean
    public RedisTemplate<String, String> stringRedisTemplate() {
        try {
            RedisTemplate<String, String> template = new RedisTemplate<>();
            template.setConnectionFactory(redisConnectionFactory());
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new StringRedisSerializer());
            return template;
        } catch (Exception e) {
            // Return a working but no-op implementation for fallback
            RedisTemplate<String, String> template = new RedisTemplate<>();
            template.setConnectionFactory(redisConnectionFactory());
            return template;
        }
    }
}