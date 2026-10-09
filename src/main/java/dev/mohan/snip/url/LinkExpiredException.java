package dev.mohan.snip.url;

public class LinkExpiredException extends RuntimeException {

    public LinkExpiredException(String code) {
        super("link " + code + " has expired");
    }
}
