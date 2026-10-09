package dev.mohan.snip.url;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LinkNotFoundException extends RuntimeException {

    public LinkNotFoundException(String code) {
        super("no link for code " + code);
    }
}
