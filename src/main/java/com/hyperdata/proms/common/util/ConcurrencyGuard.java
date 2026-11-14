package com.hyperdata.proms.common.util;

import com.hyperdata.proms.common.exception.IfMatchRequiredException;
import com.hyperdata.proms.common.exception.StaleVersionException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utility guard untuk concurrency control optimistik berbasis HTTP header If-Match dan timestamp updatedAt.
 */
public final class ConcurrencyGuard {

    private ConcurrencyGuard() {
    }

    /**
     * Memverifikasi versi updatedAt entitas terhadap header HTTP If-Match tanpa payload current.
     *
     * @param currentUpdatedAt
     *        timestamp updatedAt saat ini di database
     */
    public static void requireMatch(Instant currentUpdatedAt) {
        requireMatch(currentUpdatedAt, null);
    }

    /**
     * Memverifikasi versi updatedAt entitas terhadap header HTTP If-Match dengan lazy payload current.
     *
     * @param currentUpdatedAt
     *        timestamp updatedAt saat ini di database
     * @param currentPayloadSupplier
     *        lambda supplier data terbaru (hanya dieksekusi jika terjadi konflik)
     */
    public static void requireMatch(Instant currentUpdatedAt, Supplier<Object> currentPayloadSupplier) {
        String ifMatch = resolveIfMatchHeader();
        requireMatch(ifMatch, currentUpdatedAt, currentPayloadSupplier);
    }

    /**
     * Memverifikasi kecocokan versi dengan nilai If-Match yang diberikan secara eksplisit.
     *
     * @param ifMatchHeader
     *        nilai header If-Match dari klien
     * @param currentUpdatedAt
     *        timestamp updatedAt saat ini di database
     * @param currentPayloadSupplier
     *        lambda supplier data terbaru
     */
    public static void requireMatch(String ifMatchHeader, Instant currentUpdatedAt,
        Supplier<Object> currentPayloadSupplier) {
        if (ifMatchHeader == null || ifMatchHeader.isBlank()) {
            throw new IfMatchRequiredException();
        }

        if (currentUpdatedAt == null) {
            return;
        }

        Instant clientVersion = parseVersion(ifMatchHeader);
        Instant dbVersion = currentUpdatedAt.truncatedTo(ChronoUnit.MICROS);

        if (!dbVersion.equals(clientVersion)) {
            Object currentPayload = currentPayloadSupplier != null ? currentPayloadSupplier.get() : null;
            throw new StaleVersionException(currentPayload);
        }
    }

    private static String resolveIfMatchHeader() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            return request.getHeader(HttpHeaders.IF_MATCH);
        }
        return null;
    }

    private static Instant parseVersion(String raw) {
        String cleaned = cleanHeader(raw);
        if (cleaned.isBlank()) {
            throw new IfMatchRequiredException();
        }

        try {
            // Format 1: Epoch Milliseconds (angka bulat, e.g. "1731301234567")
            if (cleaned.matches("^\\d+$")) {
                return Instant.ofEpochMilli(Long.parseLong(cleaned)).truncatedTo(ChronoUnit.MICROS);
            }
            // Format 2: ISO-8601 String (e.g. "2026-11-10T12:00:00Z")
            return Instant.parse(cleaned).truncatedTo(ChronoUnit.MICROS);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IfMatchRequiredException();
        }
    }

    private static String cleanHeader(String raw) {
        String trimmed = raw.trim();
        // Hapus weak etag prefix W/ jika ada
        if (trimmed.startsWith("W/") || trimmed.startsWith("w/")) {
            trimmed = trimmed.substring(2).trim();
        }
        // Hapus quotes
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }
}
