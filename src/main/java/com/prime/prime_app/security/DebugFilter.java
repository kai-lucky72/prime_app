package com.prime.prime_app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // Wrap the request to be able to read the body multiple times
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        
        // Log request details
        log.debug("==== DEBUG FILTER START ====");
        log.debug("Request Method: {}", requestWrapper.getMethod());
        log.debug("Request URI: {}", requestWrapper.getRequestURI());
        log.debug("Context Path: {}", requestWrapper.getContextPath());
        log.debug("Servlet Path: {}", requestWrapper.getServletPath());
        
        // Log headers
        log.debug("Headers:");
        Enumeration<String> headerNames = requestWrapper.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.debug("  {}: {}", headerName, requestWrapper.getHeader(headerName));
        }

        // Continue with the filter chain
        log.debug("==== DEBUG FILTER PASSING REQUEST TO NEXT FILTER ====");
        filterChain.doFilter(requestWrapper, response);
        
        // Log the request body after it's been read
        if ("POST".equalsIgnoreCase(requestWrapper.getMethod()) || 
            "PUT".equalsIgnoreCase(requestWrapper.getMethod())) {
            String requestBody = new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            if (!requestBody.isEmpty()) {
                log.debug("Request body: {}", requestBody);
            }
        }
        
        log.debug("==== DEBUG FILTER END ====");
    }
} 