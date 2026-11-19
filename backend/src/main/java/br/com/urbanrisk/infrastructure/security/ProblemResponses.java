package br.com.urbanrisk.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Filters run outside MVC advice, so they write RFC 7807 bodies themselves. Messages are constants (no injection). */
final class ProblemResponses {

    private ProblemResponses() {
    }

    static void write(HttpServletResponse response, int status, String title, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status
                + ",\"detail\":\"" + detail + "\"}");
    }
}
