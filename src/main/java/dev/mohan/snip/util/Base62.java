package dev.mohan.snip.util;

public final class Base62 {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length();

    private Base62() {
    }

    public static String encode(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("negative value: " + value);
        }
        if (value == 0) {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            sb.append(ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    public static long decode(String s) {
        long result = 0;
        for (char c : s.toCharArray()) {
            int idx = ALPHABET.indexOf(c);
            if (idx < 0) {
                throw new IllegalArgumentException("not base62: " + s);
            }
            result = result * BASE + idx;
        }
        return result;
    }
}
