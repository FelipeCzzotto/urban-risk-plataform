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
import org.springframework.web.util.UrlPathHelper;

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

    private final int limit;
    private final int maxTrackedClients;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private record Window(long start, int count) {
    }

    public CommunityRateLimitFilter(@Value("${app.rate-limit.community-per-minute:10}") int limit,
                                    @Value("${app.rate-limit.max-tracked-clients:10000}") int maxTrackedClients) {
        this.limit = limit;
        this.maxTrackedClients = maxTrackedClients;
    }

    int trackedClients() {
        return windows.size();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Same normalization as MVC routing (decode, strip ;params, collapse //) so raw-URI tricks cannot skip the filter.
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        return !("POST".equals(request.getMethod()) && path.matches("^/api/v1/events/community/?$"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        String ip = request.getRemoteAddr();
        if (!windows.containsKey(ip) && windows.size() >= maxTrackedClients) {
            windows.values().removeIf(x -> now - x.start() >= WINDOW_MS);
        }
        // Table still full of live windows (e.g. IPv6 rotation flood): fail closed for unknown clients, memory stays bounded.
        // ponytail: per-address key; bucket IPv6 by /64 if rotation within one prefix becomes an issue.
        boolean full = !windows.containsKey(ip) && windows.size() >= maxTrackedClients;
        Window w = full ? new Window(now, limit + 1) : windows.compute(ip, (k, cur) ->
                cur == null || now - cur.start() >= WINDOW_MS ? new Window(now, 1) : new Window(cur.start(), cur.count() + 1));
        if (w.count() > limit) {
            response.setHeader("Retry-After", String.valueOf(Math.max(1, (WINDOW_MS - (now - w.start())) / 1000)));
            ProblemResponses.write(response, 429, "Too Many Requests", "Community report rate limit exceeded");
            return;
        }
        chain.doFilter(request, response);
    }
}
