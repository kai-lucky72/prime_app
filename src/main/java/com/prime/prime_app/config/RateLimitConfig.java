package com.prime.prime_app.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitConfig implements WebMvcConfigurer {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${rate.limit.requests.per.minute:20}")
    private int requestsPerMinute;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor())
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/**", "/api/v1/api-docs/**", "/api/v1/swagger-ui/**");
    }

    private class RateLimitInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
            String apiKey = request.getHeader("X-API-KEY");
            String clientId = apiKey != null ? apiKey : request.getRemoteAddr();
            String key = "rate_limit:" + clientId;

            // Get the current count
            String countStr = redisTemplate.opsForValue().get(key);
            long count = countStr != null ? Long.parseLong(countStr) : 0;

            if (count >= requestsPerMinute) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}");
                return false;
            }

            // Increment the counter
            if (count == 0) {
                // First request, set with expiry
                redisTemplate.opsForValue().set(key, "1", Duration.ofMinutes(1));
            } else {
                // Increment existing counter
                redisTemplate.opsForValue().increment(key);
            }

            return true;
        }
    }
}