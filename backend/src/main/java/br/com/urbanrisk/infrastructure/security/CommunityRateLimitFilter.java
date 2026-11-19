package br.com.urbanrisk.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed one-minute window per client IP for POST /api/v1/events/community.
 * ponytail: in-memory, single instance, keyed on remoteAddr (X-Forwarded-For ignored: spoofable).
 * Move to Redis/bucket4j + trusted proxy config when running more than one replica behind a proxy.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CommunityRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int limit;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private record Window(long start, int count) {
    }

    public CommunityRateLimitFilter(@Value("${app.rate-limit.community-per-minute:10}") int limit) {
        this.limit = limit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !("POST".equals(request.getMethod()) && path.matches("^/api/v1/events/community/?$"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(w -> now - w.start() >= WINDOW_MS);
        }
        Window w = windows.compute(request.getRemoteAddr(), (k, cur) ->
                cur == null || now - cur.start() >= WINDOW_MS ? new Window(now, 1) : new Window(cur.start(), cur.count() + 1));
        if (w.count() > limit) {
            response.setHeader("Retry-After", String.valueOf(Math.max(1, (WINDOW_MS - (now - w.start())) / 1000)));
            ProblemResponses.write(response, 429, "Too Many Requests", "Community report rate limit exceeded");
            return;
        }
        chain.doFilter(request, response);
    }
}
