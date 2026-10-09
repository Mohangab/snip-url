package dev.mohan.snip.url;

import dev.mohan.snip.util.Base62;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UrlService {

    // start ids at a bigger number so codes aren't "1", "2", ...
    private static final long ID_OFFSET = 1_000_000L;

    private final ShortUrlRepository repo;

    public UrlService(ShortUrlRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public ShortUrl shorten(String url) {
        ShortUrl s = repo.save(new ShortUrl(url, Instant.now()));
        s.setCode(Base62.encode(s.getId() + ID_OFFSET));
        return s;
    }
}
