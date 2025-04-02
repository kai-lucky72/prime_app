package com.prime.prime_app.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class MetricsConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    private static final Set<String> IGNORE_PATTERN = new HashSet<>(Arrays.asList(
        "/api-docs.*",
        "/swagger-ui.*",
        "/actuator.*"
    ));

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    public MeterFilter httpMetricsFilter() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                String uri = id.getTag("uri");
                if (uri != null && shouldIgnore(uri)) {
                    return id.withTags(Tags.empty());
                }
                return id.withTag(Tag.of("application", applicationName));
            }
        };
    }

    private String getPattern(HttpServletRequest request) {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern != null ? pattern : request.getRequestURI();
    }

    private boolean shouldIgnore(String pattern) {
        return IGNORE_PATTERN.stream().anyMatch(pattern::matches);
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
            "application", applicationName,
            "env", System.getProperty("spring.profiles.active", "default")
        );
    }
}