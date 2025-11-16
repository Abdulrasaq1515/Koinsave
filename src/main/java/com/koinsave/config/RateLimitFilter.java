package com.koinsave.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!rateLimitEnabled || shouldSkipRateLimit(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = getClientIdentifier(request);
        RequestCounter counter = requestCounts.computeIfAbsent(clientId, k -> new RequestCounter());

        if (!counter.allowRequest()) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"message\":\"Too many requests. Please try again later.\",\"status\":429}"
            );
            return;
        }
        filterChain.doFilter(request, response);
    }
    private boolean shouldSkipRateLimit(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/auth");
    }

    private String getClientIdentifier(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Authorization"))
                .filter(header -> header.startsWith("Bearer "))
                .orElseGet(() -> Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                        .filter(ip -> !ip.isEmpty())
                        .orElse(request.getRemoteAddr()));
    }

    private class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private long lastResetTime = System.currentTimeMillis();

        public synchronized boolean allowRequest() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastResetTime > 60000) { // 1 minute
                count.set(0);
                lastResetTime = currentTime;
            }
            return count.incrementAndGet() <= requestsPerMinute;
        }
    }
}