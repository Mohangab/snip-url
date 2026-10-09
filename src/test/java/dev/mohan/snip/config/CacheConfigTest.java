package dev.mohan.snip.config;

import dev.mohan.snip.url.CachedLink;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    // tests run with the simple cache so make sure the redis serializer actually round trips
    @Test
    void cachedLinkRoundTripsThroughRedisSerializer() {
        RedisCacheConfiguration cfg = new CacheConfig().redisCacheConfiguration(Duration.ofMinutes(5));
        CachedLink link = new CachedLink("https://example.com", Instant.parse("2030-01-01T10:15:30Z"));

        ByteBuffer bytes = cfg.getValueSerializationPair().write(link);
        Object back = cfg.getValueSerializationPair().read(bytes);

        assertThat(back).isEqualTo(link);
        assertThat(cfg.getTtlFunction().getTimeToLive(link, link)).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    void nullExpiryIsFine() {
        RedisCacheConfiguration cfg = new CacheConfig().redisCacheConfiguration(Duration.ofMinutes(5));
        CachedLink link = new CachedLink("https://example.com", null);
        assertThat(cfg.getValueSerializationPair().read(cfg.getValueSerializationPair().write(link))).isEqualTo(link);
    }
}
