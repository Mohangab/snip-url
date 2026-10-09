package dev.mohan.snip.url;

import java.time.Instant;
import java.util.Map;

public record LinkStats(
        String code,
        String url,
        long clicks,
        Instant createdAt,
        Instant lastAccessedAt,
        Instant expiresAt,
        Map<String, Long> topReferrers,
        Map<String, Long> topUserAgents) {
}
