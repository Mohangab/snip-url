package dev.mohan.snip.url;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatsTest {

    @Autowired
    MockMvc mvc;

    @Test
    void countsClicks() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/stats\",\"customAlias\":\"statme\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/statme").header("Referer", "https://news.ycombinator.com").header("User-Agent", "curl/8.0"));
        mvc.perform(get("/statme").header("Referer", "https://news.ycombinator.com").header("User-Agent", "curl/8.0"));
        mvc.perform(get("/statme").header("User-Agent", "Mozilla/5.0"));

        // clicks are recorded async so give it a moment
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> mvc.perform(get("/api/urls/statme/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clicks").value(3))
                .andExpect(jsonPath("$.lastAccessedAt").exists())
                .andExpect(jsonPath("$.topReferrers['https://news.ycombinator.com']").value(2))
                .andExpect(jsonPath("$.topReferrers.direct").value(1))
                .andExpect(jsonPath("$.topUserAgents['curl/8.0']").value(2)));
    }

    @Test
    void statsForUnknownCodeIs404() throws Exception {
        mvc.perform(get("/api/urls/nope-nope/stats")).andExpect(status().isNotFound());
    }
}
