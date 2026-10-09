package dev.mohan.snip.url;

import dev.mohan.snip.util.Base62;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class UrlService {

    // start ids at a bigger number so codes aren't "1", "2", ...
    private static final long ID_OFFSET = 1_000_000L;

    private static final Set<String> RESERVED = Set.of("api", "actuator", "health", "admin");

    private final ShortUrlRepository repo;
    private final ClickEventRepository clicks;

    public UrlService(ShortUrlRepository repo, ClickEventRepository clicks) {
        this.repo = repo;
        this.clicks = clicks;
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

    @Transactional(readOnly = true)
    public String resolve(String code) {
        ShortUrl s = repo.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
        if (s.getExpiresAt() != null && s.getExpiresAt().isBefore(Instant.now())) {
            throw new LinkExpiredException(code);
        }
        return s.getOriginalUrl();
    }

    @Async
    @Transactional
    public void recordClick(String code, String referrer, String userAgent) {
        Instant now = Instant.now();
        repo.incrementClicks(code, now);
        clicks.save(new ClickEvent(code, truncate(referrer), truncate(userAgent), now));
    }

    @Transactional(readOnly = true)
    public LinkStats stats(String code) {
        ShortUrl s = repo.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
        return new LinkStats(s.getCode(), s.getOriginalUrl(), s.getClicks(), s.getCreatedAt(),
                s.getLastAccessedAt(), s.getExpiresAt(),
                top(clicks.countByReferrer(code)), top(clicks.countByUserAgent(code)));
    }

    private static Map<String, Long> top(List<Object[]> rows) {
        Map<String, Long> out = new LinkedHashMap<>();
        rows.stream().limit(5).forEach(r -> out.put((String) r[0], (Long) r[1]));
        return out;
    }

    private static String truncate(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.length() > 512 ? s.substring(0, 512) : s;
    }
}
