package dev.mohan.snip.url;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LinkCache {

    public static final String CACHE = "links";

    private final ShortUrlRepository repo;

    public LinkCache(ShortUrlRepository repo) {
        this.repo = repo;
    }

    // not found throws so misses never get cached
    @Cacheable(CACHE)
    @Transactional(readOnly = true)
    public CachedLink lookup(String code) {
        ShortUrl s = repo.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
        return new CachedLink(s.getOriginalUrl(), s.getExpiresAt());
    }
}
