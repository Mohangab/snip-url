package dev.mohan.snip.url;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClickService {

    private static final Logger log = LoggerFactory.getLogger(ClickService.class);
    private static final int MAX_HEADER_LEN = 512;

    private final ShortUrlRepository urls;
    private final ClickEventRepository clicks;

    public ClickService(ShortUrlRepository urls, ClickEventRepository clicks) {
        this.urls = urls;
        this.clicks = clicks;
    }

    /**
     * Fire and forget - runs on the async executor so the redirect doesn't wait on the db write.
     * If this fails we just lose a click, not the end of the world.
     */
    @Async
    @Transactional
    public void recordClick(String code, String referrer, String userAgent) {
        Instant now = Instant.now();
        int updated = urls.incrementClicks(code, now);
        if (updated == 0) {
            log.warn("click for unknown code {}", code);
            return;
        }
        clicks.save(new ClickEvent(code, truncate(referrer), truncate(userAgent), now));
    }

    // tried buffering clicks in memory and flushing every few secs, works but loses clicks on restart.
    // not worth it at this traffic level (i.e. zero)
    //
    // private final Map<String, LongAdder> pending = new ConcurrentHashMap<>();
    //
    // @Scheduled(fixedDelay = 5000)
    // void flush() {
    //     pending.forEach((code, n) -> urls.incrementClicksBy(code, n.sumThenReset(), Instant.now()));
    // }

    @Transactional(readOnly = true)
    public LinkStats stats(String code) {
        ShortUrl s = urls.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
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
        return s.length() > MAX_HEADER_LEN ? s.substring(0, MAX_HEADER_LEN) : s;
    }
}
