package dev.mohan.snip.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "snip.rate-limit.creates-per-minute=3")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitTest {

    @Autowired
    MockMvc mvc;

    @Test
    void fourthCreateInAMinuteIs429() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"url\":\"https://example.com/rl" + i + "\"}"))
                    .andExpect(status().isCreated());
        }
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/rl-too-many\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }

    @Test
    void windowResets() throws Exception {
        MutableClock clock = new MutableClock();
        CreateRateLimiter limiter = new CreateRateLimiter(1, clock);
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/urls");

        assertThat(limiter.preHandle(req, new MockHttpServletResponse(), null)).isTrue();
        assertThat(limiter.preHandle(req, new MockHttpServletResponse(), null)).isFalse();

        clock.now = clock.now.plusSeconds(61);
        assertThat(limiter.preHandle(req, new MockHttpServletResponse(), null)).isTrue();
    }

    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
