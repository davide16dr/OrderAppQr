package com.orderapp.ordering.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Per-IP rate limiting on public endpoints to prevent brute-force and abuse.
 * Uses Bucket4j token-bucket algorithm with Caffeine for in-memory bucket storage.
 *
 * Limits (per IP):
 *   auth endpoints      →  10 req/min   (brute-force prevention)
 *   POST customer/orders→  30 req/min   (order spam prevention)
 *   GET  customer/*     → 200 req/min   (menu browsing)
 *   business-reg/signup →   5 req/hour  (fake registration prevention)
 */
@Slf4j
@Component
@Order(-100)
public class RateLimitFilter extends OncePerRequestFilter {

    private record BucketSpec(String category, long capacity, long refill, Duration window) {}

    // Keyed by "ip:category" — evicted after 10 min inactivity
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        BucketSpec spec = resolveSpec(path, method);
        if (spec == null) {
            chain.doFilter(request, response);
            return;
        }

        String ip = extractIp(request);
        Bucket bucket = buckets.get(ip + ":" + spec.category(), k -> newBucket(spec));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded: ip={} path={} method={}", ip, path, method);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"error\":\"Too many requests\",\"retryAfter\":60}");
        }
    }

    private BucketSpec resolveSpec(String path, String method) {
        if (path.startsWith("/api/public/auth/")) {
            return new BucketSpec("auth", 10, 10, Duration.ofMinutes(1));
        }
        if (path.startsWith("/api/public/business-registration/signup")) {
            return new BucketSpec("signup", 5, 5, Duration.ofHours(1));
        }
        if ("/api/public/customer/orders".equals(path) && "POST".equals(method)) {
            return new BucketSpec("orders", 30, 30, Duration.ofMinutes(1));
        }
        if (path.startsWith("/api/public/customer/")) {
            return new BucketSpec("menu", 200, 200, Duration.ofMinutes(1));
        }
        return null;
    }

    private Bucket newBucket(BucketSpec spec) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(spec.capacity())
                        .refillGreedy(spec.refill(), spec.window())
                        .build())
                .build();
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
