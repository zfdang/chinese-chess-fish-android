package com.zfdang.chess.utils;

/** String.strip() semantics using APIs available on our minimum Android version. */
public final class TextUtil {
    private TextUtil() {}

    public static String stripWhitespace(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && Character.isWhitespace(text.charAt(start))) start++;
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) end--;
        return text.substring(start, end);
    }
}
