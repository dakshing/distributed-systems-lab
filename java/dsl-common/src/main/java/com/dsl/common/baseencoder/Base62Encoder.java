package com.dsl.common.baseencoder;

import java.util.Arrays;

/**
 * Utility to convert between unique Long IDs and short Base62 Strings.
 * <p>
 * Alphabet: [0-9, a-z, A-Z]
 */
public class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length(); // 62

    // Pre-computed map for fast (char -> value) decoding
    private static final int[] INDEX_MAP = new int[128];

    static {
        Arrays.fill(INDEX_MAP, -1);
        for (int i = 0; i < BASE; i++) {
            INDEX_MAP[ALPHABET.charAt(i)] = i;
        }
    }

    /**
     * Encodes a numeric ID into a Base62 string.
     * Example: 1024 -> "g8"
     */
    public static String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("ID must be non-negative");
        }
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            int remainder = (int) (id % BASE);
            sb.append(ALPHABET.charAt(remainder));
            id /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back into a numeric ID.
     * Example: "g8" -> 1024
     */
    public static long decode(String str) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("String cannot be null or empty");
        }

        long id = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int val = (c < INDEX_MAP.length) ? INDEX_MAP[c] : -1;

            if (val == -1) {
                throw new IllegalArgumentException("Invalid character in Base62 string: " + c);
            }

            id = id * BASE + val;
        }
        return id;
    }
}