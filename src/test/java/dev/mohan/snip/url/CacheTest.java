package dev.mohan.snip.url;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CacheTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void secondRedirectComesFromCache() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/cached\",\"customAlias\":\"cache-me\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/cache-me")).andExpect(status().isFound());

        // sneakily change the row underneath, cached value should still win
        jdbc.update("update short_urls set original_url = ? where code = ?", "https://example.com/changed", "cache-me");

        mvc.perform(get("/cache-me"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/cached"));
    }

    @Test
    void missesAreNotCached() throws Exception {
        mvc.perform(get("/not-yet")).andExpect(status().isNotFound());

        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/late\",\"customAlias\":\"not-yet\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/not-yet")).andExpect(status().isFound());
    }
}
