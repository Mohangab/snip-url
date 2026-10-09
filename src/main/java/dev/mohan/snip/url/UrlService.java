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

    public UrlService(ShortUrlRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ShortUrl shorten(String url, String customAlias) {
        validateUrl(url);
        if (customAlias != null && !customAlias.isBlank()) {
            if (RESERVED.contains(customAlias.toLowerCase(Locale.ROOT)) || repo.existsByCode(customAlias)) {
                throw new AliasTakenException(customAlias);
            }
            ShortUrl s = new ShortUrl(url, Instant.now());
            s.setCode(customAlias);
            return repo.save(s);
        }
        ShortUrl s = repo.save(new ShortUrl(url, Instant.now()));
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
            uri = new URI(url.trim());
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
        return repo.findByCode(code)
                .map(ShortUrl::getOriginalUrl)
                .orElseThrow(() -> new LinkNotFoundException(code));
    }
}
