package dev.mohan.snip.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dumb fixed-window limiter for POST /api/urls, per client ip.
 * In-memory only so it's per instance.
 * TODO: move this to redis (INCR + EXPIRE) if this ever runs on more than one box
 */
@Component
public class CreateRateLimiter implements HandlerInterceptor {

    private static final long WINDOW_MS = 60_000;

    private final int limit;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public CreateRateLimiter(@Value("${snip.rate-limit.creates-per-minute:30}") int limit) {
        this(limit, Clock.systemUTC());
    }

    CreateRateLimiter(int limit, Clock clock) {
        this.limit = limit;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws IOException {
        if (!"POST".equalsIgnoreCase(req.getMethod())) {
            return true;
        }
        long now = clock.millis();
        String key = req.getRemoteAddr(); // not trusting X-Forwarded-For for now
        Window w = windows.compute(key, (k, old) ->
                old == null || now - old.start >= WINDOW_MS ? new Window(now, 1) : new Window(old.start, old.count + 1));
        if (w.count > limit) {
            long retryAfter = Math.max(1, (w.start + WINDOW_MS - now) / 1000);
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setHeader("Retry-After", String.valueOf(retryAfter));
            res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            res.getWriter().write("{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"slow down\"}");
            return false;
        }
        // old windows just hang around, fine for now
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> now - e.getValue().start >= WINDOW_MS);
        }
        return true;
    }

    private record Window(long start, int count) {
    }
}
