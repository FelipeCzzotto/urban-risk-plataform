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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Pattern;

/** Requires X-API-Key on ingestion (sensor/public) and resolve. Fails closed when no key is configured. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";
    private static final Pattern RESOLVE = Pattern.compile("^/api/v1/events/[^/]+/resolve/?$");

    private final byte[] expected;

    public ApiKeyFilter(@Value("${app.ingest-api-key:}") String apiKey) {
        this.expected = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        boolean ingest = "POST".equals(method)
                && (path.matches("^/api/v1/events/(sensor|public)/?$"));
        boolean resolve = "PATCH".equals(method) && RESOLVE.matcher(path).matches();
        return !(ingest || resolve);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (expected.length == 0 || provided == null
                || !MessageDigest.isEqual(expected, provided.getBytes(StandardCharsets.UTF_8))) {
            ProblemResponses.write(response, 401, "Unauthorized", "Missing or invalid X-API-Key");
            return;
        }
        chain.doFilter(request, response);
    }
}
