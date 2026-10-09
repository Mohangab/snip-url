package dev.mohan.snip.url;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    public CreateResponse create(@Valid @RequestBody CreateRequest req) {
        ShortUrl s = service.shorten(req.url(), req.customAlias());
        return new CreateResponse(s.getCode(), baseUrl + "/" + s.getCode(), s.getOriginalUrl());
    }

    public record CreateRequest(
            @NotBlank @Size(max = 2048) String url,
            @Pattern(regexp = "^[A-Za-z0-9-]{3,32}$", message = "must be 3-32 chars, letters/digits/dash") String customAlias) {
    }

    public record CreateResponse(String code, String shortUrl, String url) {
    }
}
