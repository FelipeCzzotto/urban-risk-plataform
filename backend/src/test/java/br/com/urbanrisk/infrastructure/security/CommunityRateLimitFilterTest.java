package br.com.urbanrisk.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityRateLimitFilterTest {

    private static int post(CommunityRateLimitFilter f, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/events/community");
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void trackedClientsStayBoundedUnderIpRotation() throws Exception {
        CommunityRateLimitFilter f = new CommunityRateLimitFilter(10, 100);
        for (int i = 0; i < 1_000; i++) {
            post(f, "2001:db8::" + Integer.toHexString(i));
        }
        assertThat(f.trackedClients()).isLessThanOrEqualTo(100);
    }

    @Test
    void knownClientsKeepTheirQuotaWhenTableIsFull() throws Exception {
        CommunityRateLimitFilter f = new CommunityRateLimitFilter(2, 3);
        for (int i = 0; i < 3; i++) post(f, "10.0.0." + i);
        assertThat(post(f, "10.0.0.99")).isEqualTo(429); // full of live windows: new clients fail closed
        assertThat(post(f, "10.0.0.0")).isEqualTo(200);
        assertThat(post(f, "10.0.0.0")).isEqualTo(429);
    }
}
