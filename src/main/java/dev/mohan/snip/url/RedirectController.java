package dev.mohan.snip.url;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class RedirectController {

    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);

    private final UrlService service;
    private final ClickService clickService;

    public RedirectController(UrlService service, ClickService clickService) {
        this.service = service;
        this.clickService = clickService;
    }

    @GetMapping("/{code:[A-Za-z0-9_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code,
                                         @RequestHeader(value = HttpHeaders.REFERER, required = false) String referrer,
                                         @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        String target = service.resolve(code);
        log.info("redirect {} -> {} (ref={}, ua={})", code, target, referrer, userAgent);
        clickService.recordClick(code, referrer, userAgent);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(target)).build();
    }
}
