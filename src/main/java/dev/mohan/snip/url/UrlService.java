package dev.mohan.snip.url;

import dev.mohan.snip.util.Base62;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
public class UrlService {

    // start ids at a bigger number so codes aren't "1", "2", ...
    private static final long ID_OFFSET = 1_000_000L;

    private static final Set<String> RESERVED = Set.of("api", "actuator", "health", "admin");

    private final ShortUrlRepository repo;
    private final LinkCache linkCache;

    public UrlService(ShortUrlRepository repo, LinkCache linkCache) {
        this.repo = repo;
        this.linkCache = linkCache;
    }

    @Transactional
    public ShortUrl shorten(String rawUrl, String customAlias, Instant expiresAt) {
        String url = rawUrl.trim();
        validateUrl(url);
        if (expiresAt != null && !expiresAt.isAfter(Instant.now())) {
            throw new InvalidUrlException("expiresAt must be in the future");
        }
        if (customAlias != null && !customAlias.isBlank()) {
            if (RESERVED.contains(customAlias.toLowerCase(Locale.ROOT)) || repo.existsByCode(customAlias)) {
                throw new AliasTakenException(customAlias);
            }
            ShortUrl s = new ShortUrl(url, Instant.now());
            s.setCode(customAlias);
            s.setExpiresAt(expiresAt);
            return repo.save(s);
        }
        ShortUrl fresh = new ShortUrl(url, Instant.now());
        fresh.setExpiresAt(expiresAt);
        ShortUrl s = repo.save(fresh);
        String code = Base62.encode(s.getId() + ID_OFFSET);
        // aliases can't contain '_' so this can never clash with one
        // TODO: this is kinda hacky, maybe just keep aliases in their own namespace
        if (RESERVED.contains(code) || repo.existsByCode(code)) {
            code = code + "_";
        }
        s.setCode(code);
        return s;
    }

    static void validateUrl(String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("not a valid url");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new InvalidUrlException("only http/https urls are supported");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new InvalidUrlException("url has no host");
        }
        // TODO: block links pointing back at ourselves (redirect loops)
    }

    public String resolve(String code) {
        CachedLink link = linkCache.lookup(code);
        // expiry is checked on every hit, the cache ttl doesn't know about expiresAt
        if (link.expiresAt() != null && link.expiresAt().isBefore(Instant.now())) {
            throw new LinkExpiredException(code);
        }
        return link.url();
    }
}
