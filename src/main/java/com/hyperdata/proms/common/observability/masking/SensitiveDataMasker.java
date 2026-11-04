package com.hyperdata.proms.common.observability.masking;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {

    private static final String DEFAULT_KEYS = "password|token|secret|accessToken|refreshToken|apiKey|authorization|pin|clientSecret";

    private static final Pattern PEM_KEY_PATTERN = Pattern.compile(
        "-----BEGIN (?:[A-Z0-9_-]+ )?PRIVATE KEY-----[\\s\\S]+?-----END (?:[A-Z0-9_-]+ )?PRIVATE KEY-----");

    private static final Pattern JWT_PATTERN = Pattern.compile(
        "\\b(eyJ[A-Za-z0-9_-]{10,}\\.eyJ[A-Za-z0-9_-]{10,}\\.)[A-Za-z0-9_-]+\\b");

    private static final Pattern NIK_PATTERN = Pattern.compile(
        "(?i)\\b(nik|ktp|identityNumber)\\s*[:=]\\s*[\"']?(\\d{4})\\d{8}(\\d{4})[\"']?\\b");

    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "\\b(?:\\d{4}[- ]?){3}(\\d{4})\\b");

    private static final Pattern CVV_PATTERN = Pattern.compile(
        "(?i)\\b(cvv|cvc|cid)\\s*[:=]\\s*[\"']?\\d{3,4}[\"']?\\b");

    private static final Pattern NPWP_PATTERN = Pattern.compile(
        "\\b(\\d{2})\\.\\d{3}\\.\\d{3}\\.\\d-\\d{3}\\.(\\d{3})\\b");

    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(?:(?<=\\s)|^|\\b)(\\+?62|08)(\\d{2,3})\\d{4,8}(\\d{2})\\b");

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b([A-Za-z0-9])[A-Za-z0-9._%+-]*([A-Za-z0-9])@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b");

    private SensitiveDataMasker() {
    }

    public static String mask(String input) {
        return mask(input, Collections.emptySet());
    }

    public static String mask(String input, Set<String> customKeys) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = input;

        // 1. Mask PEM Private Keys
        result = PEM_KEY_PATTERN.matcher(result).replaceAll("[MASKED_PRIVATE_KEY]");

        // 2. Mask JWT Signatures
        result = JWT_PATTERN.matcher(result).replaceAll("eyJ***MASKED_JWT***");

        // 3. Mask Key-Value credentials (quoted & unquoted)
        String combinedKeys = DEFAULT_KEYS;
        if (customKeys != null && !customKeys.isEmpty()) {
            combinedKeys = DEFAULT_KEYS + "|" + String.join("|", customKeys);
        }
        Pattern quotedPattern = Pattern.compile(
            "(?i)(\"?(?:" + combinedKeys + ")\"?\\s*[:=]\\s*([\"']))(.*?)\\2");
        result = quotedPattern.matcher(result).replaceAll("$1***MASKED***$2");

        Pattern unquotedPattern = Pattern.compile(
            "(?i)(\"?(?:" + combinedKeys + ")\"?\\s*[:=]\\s*)([^\"'\\s,;]+)");
        result = unquotedPattern.matcher(result).replaceAll("$1***MASKED***");

        // 4. Mask NIK (before generic 16-digit credit card pattern)
        result = NIK_PATTERN.matcher(result).replaceAll("$1: $2************");

        // 5. Mask CVV
        result = CVV_PATTERN.matcher(result).replaceAll("$1: ***MASKED_CVV***");

        // 6. Mask Credit Card (Preserve last 4)
        result = CREDIT_CARD_PATTERN.matcher(result).replaceAll("****-****-****-$1");

        // 7. Mask NPWP
        result = NPWP_PATTERN.matcher(result).replaceAll("$1.***.***.*-***.$2");

        // 8. Mask Phone Number
        result = PHONE_PATTERN.matcher(result).replaceAll("$1$2******$3");

        // 9. Mask Email
        result = EMAIL_PATTERN.matcher(result).replaceAll("$1****$2@$3");

        return result;
    }
}
