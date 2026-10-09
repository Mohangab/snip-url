package dev.mohan.snip.url;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/urls")
public class UrlController {

    private final UrlService service;
    private final String baseUrl;

    public UrlController(UrlService service, @Value("${snip.base-url}") String baseUrl) {
        this.service = service;
        this.baseUrl = baseUrl;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateResponse create(@RequestBody CreateRequest req) {
        ShortUrl s = service.shorten(req.url());
        return new CreateResponse(s.getCode(), baseUrl + "/" + s.getCode(), s.getOriginalUrl());
    }

    public record CreateRequest(String url) {
    }

    public record CreateResponse(String code, String shortUrl, String url) {
    }
}
