package com.jobportal.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * A fixed-window rate limiter backed by Redis, applied only to /api/v1/auth/**
 * -- the highest-risk surface for brute-force login attempts and
 * registration spam. Each client IP gets a counter key that expires after
 * 60 seconds; Redis's INCR is atomic, so concurrent requests from the same
 * IP can't race past the limit the way a naive read-then-write counter
 * could.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (!request.getRequestURI().startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = "ratelimit:auth:" + clientIp(request);

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            // Only set the expiry on the FIRST increment in a window --
            // resetting it on every request would let a steady stream of
            // traffic keep pushing the window forward indefinitely.
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        if (count != null && count > requestsPerMinute) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                            + "\"message\":\"Rate limit exceeded. Please try again in a minute.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}