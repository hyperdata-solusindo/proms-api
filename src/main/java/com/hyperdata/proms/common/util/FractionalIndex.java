package com.hyperdata.proms.common.util;

public final class FractionalIndex {

    public static final String FIRST_KEY = "a0";

    private static final String DIGITS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final char SMALLEST_INTEGER_HEAD = 'A';
    private static final char LARGEST_INTEGER_HEAD = 'z';

    private FractionalIndex() {
    }

    public static String keyAfter(String lastKey) {
        if (lastKey == null || lastKey.isEmpty()) {
            return FIRST_KEY;
        }
        String integerPart = integerPart(lastKey);
        String fractionPart = lastKey.substring(integerPart.length());
        String incremented = incrementInteger(integerPart);
        return incremented != null ? incremented : integerPart + midpointToEnd(fractionPart);
    }

    private static String integerPart(String key) {
        int length = integerLength(key.charAt(0));
        if (length > key.length()) {
            throw new IllegalArgumentException("Kunci fractional index tidak valid: " + key);
        }
        for (int i = 1; i < key.length(); i++) {
            if (DIGITS.indexOf(key.charAt(i)) < 0) {
                throw new IllegalArgumentException("Kunci fractional index tidak valid: " + key);
            }
        }
        return key.substring(0, length);
    }

    private static int integerLength(char head) {
        if (head >= 'a' && head <= 'z') {
            return head - 'a' + 2;
        }
        if (head >= SMALLEST_INTEGER_HEAD && head <= 'Z') {
            return 'Z' - head + 2;
        }
        throw new IllegalArgumentException("Kepala kunci fractional index tidak valid: " + head);
    }

    /** Naikkan bagian integer satu langkah; {@code null} bila sudah mencapai integer terbesar. */
    private static String incrementInteger(String integerPart) {
        char head = integerPart.charAt(0);
        StringBuilder digits = new StringBuilder(integerPart.substring(1));
        for (int i = digits.length() - 1; i >= 0; i--) {
            int next = DIGITS.indexOf(digits.charAt(i)) + 1;
            if (next < DIGITS.length()) {
                digits.setCharAt(i, DIGITS.charAt(next));
                return head + digits.toString();
            }
            digits.setCharAt(i, DIGITS.charAt(0));
        }
        // carry melewati kepala: integer bertambah panjang (atau pindah dari negatif ke positif)
        if (head == 'Z') {
            return FIRST_KEY;
        }
        if (head == LARGEST_INTEGER_HEAD) {
            return null;
        }
        char nextHead = (char) (head + 1);
        if (nextHead > 'a') {
            digits.append(DIGITS.charAt(0));
        } else {
            digits.deleteCharAt(digits.length() - 1);
        }
        return nextHead + digits.toString();
    }

    /** Pecahan di antara {@code fraction} dan batas atas (tak hingga). */
    private static String midpointToEnd(String fraction) {
        int digitA = fraction.isEmpty() ? 0 : DIGITS.indexOf(fraction.charAt(0));
        int digitB = DIGITS.length();
        if (digitB - digitA > 1) {
            return String.valueOf(DIGITS.charAt(Math.round(0.5f * (digitA + digitB))));
        }
        return DIGITS.charAt(digitA) + midpointToEnd(fraction.substring(1));
    }
}
