package dev.mohan.snip.url;

public class AliasTakenException extends RuntimeException {

    public AliasTakenException(String alias) {
        super("alias already taken: " + alias);
    }
}
