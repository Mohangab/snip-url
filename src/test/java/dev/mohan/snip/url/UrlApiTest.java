package dev.mohan.snip.url;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    private JsonNode create(String body) throws Exception {
        String json = mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return om.readTree(json);
    }

    @Test
    void createAndRedirect() throws Exception {
        JsonNode res = create("{\"url\":\"https://example.com/some/long/path?x=1\"}");
        String code = res.get("code").asText();

        mvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/some/long/path?x=1"));
    }

    @Test
    void generatedCodesAreDifferent() throws Exception {
        String a = create("{\"url\":\"https://example.com/a\"}").get("code").asText();
        String b = create("{\"url\":\"https://example.com/b\"}").get("code").asText();
        org.assertj.core.api.Assertions.assertThat(a).isNotEqualTo(b);
    }

    @Test
    void customAlias() throws Exception {
        JsonNode res = create("{\"url\":\"https://github.com\",\"customAlias\":\"gh-home\"}");
        org.assertj.core.api.Assertions.assertThat(res.get("code").asText()).isEqualTo("gh-home");
        org.assertj.core.api.Assertions.assertThat(res.get("shortUrl").asText()).endsWith("/gh-home");

        mvc.perform(get("/gh-home"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://github.com"));
    }

    @Test
    void duplicateAliasIsConflict() throws Exception {
        create("{\"url\":\"https://example.com\",\"customAlias\":\"dupe-me\"}");

        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.org\",\"customAlias\":\"dupe-me\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void reservedAliasRejected() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.org\",\"customAlias\":\"actuator\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void badAliasIs400() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.org\",\"customAlias\":\"no spaces!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidUrls() throws Exception {
        for (String bad : new String[]{"", "not a url", "ftp://example.com/file", "javascript:alert(1)", "http://"}) {
            mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                            .content(om.writeValueAsString(java.util.Map.of("url", bad))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").exists());
        }
    }

    @Test
    void unknownCodeIs404() throws Exception {
        mvc.perform(get("/doesnotexist")).andExpect(status().isNotFound());
    }
}
