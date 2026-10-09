package dev.mohan.snip.url;

import java.time.Instant;

/** what we keep in redis for a code - just enough to redirect */
public record CachedLink(String url, Instant expiresAt) {
}
