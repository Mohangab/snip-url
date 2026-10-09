package dev.mohan.snip.url;

public class LinkNotFoundException extends RuntimeException {

    public LinkNotFoundException(String code) {
        super("no link for code " + code);
    }
}
