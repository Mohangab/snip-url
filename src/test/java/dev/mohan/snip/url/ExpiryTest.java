package dev.mohan.snip.url;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpiryTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void notYetExpiredStillRedirects() throws Exception {
        String future = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"customAlias\":\"later\",\"expiresAt\":\"" + future + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").exists());

        mvc.perform(get("/later")).andExpect(status().isFound());
    }

    @Test
    void expiredLinkIs410() throws Exception {
        String future = Instant.now().plus(1, ChronoUnit.HOURS).toString();
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"customAlias\":\"old-news\",\"expiresAt\":\"" + future + "\"}"))
                .andExpect(status().isCreated());

        // fast forward: push expiry into the past directly in the db
        jdbc.update("update short_urls set expires_at = ? where code = ?",
                Timestamp.from(Instant.now().minus(1, ChronoUnit.MINUTES)), "old-news");

        mvc.perform(get("/old-news")).andExpect(status().isGone());
    }

    @Test
    void expiresAtInPastRejected() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\",\"expiresAt\":\"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
    }
}
