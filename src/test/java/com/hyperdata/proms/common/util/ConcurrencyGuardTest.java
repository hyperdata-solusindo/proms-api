package com.hyperdata.proms.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hyperdata.proms.common.exception.IfMatchRequiredException;
import com.hyperdata.proms.common.exception.StaleVersionException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ConcurrencyGuardTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Nested
    @DisplayName("Explicit If-Match Tests")
    class ExplicitIfMatchTests {

        @Test
        @DisplayName("GIVEN matching ISO timestamp WHEN requireMatch THEN succeeds and supplier is not invoked")
        void givenMatchingIso_whenRequireMatch_thenSucceeds() {
            Instant now = Instant.parse("2026-11-10T10:15:30.123456Z");
            AtomicBoolean supplierCalled = new AtomicBoolean(false);

            assertThatCode(() -> ConcurrencyGuard.requireMatch(
                "2026-11-10T10:15:30.123456Z",
                now,
                () -> {
                    supplierCalled.set(true);
                    return "payload";
                })).doesNotThrowAnyException();

            assertThat(supplierCalled).isFalse();
        }

        @Test
        @DisplayName("GIVEN matching quoted ETag and weak prefix WHEN requireMatch THEN succeeds")
        void givenQuotedAndWeakEtag_whenRequireMatch_thenSucceeds() {
            Instant now = Instant.parse("2026-11-10T10:15:30.123456Z");

            assertThatCode(() -> ConcurrencyGuard.requireMatch(
                "W/\"2026-11-10T10:15:30.123456Z\"",
                now,
                null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN matching epoch millis WHEN requireMatch THEN succeeds")
        void givenMatchingEpochMillis_whenRequireMatch_thenSucceeds() {
            Instant now = Instant.ofEpochMilli(1731301234000L);

            assertThatCode(() -> ConcurrencyGuard.requireMatch(
                "\"1731301234000\"",
                now,
                null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN mismatching version WHEN requireMatch THEN throws StaleVersionException and calls supplier")
        void givenMismatchingVersion_whenRequireMatch_thenThrowsStaleVersionException() {
            Instant dbTime = Instant.parse("2026-11-10T10:15:30Z");
            String clientHeader = "2026-11-10T09:00:00Z";
            String expectedPayload = "current-data";

            assertThatThrownBy(() -> ConcurrencyGuard.requireMatch(
                clientHeader,
                dbTime,
                () -> expectedPayload))
                .isInstanceOf(StaleVersionException.class)
                .satisfies(ex -> {
                    StaleVersionException sve = (StaleVersionException) ex;
                    assertThat(sve.getCurrent()).isEqualTo(expectedPayload);
                });
        }

        @Test
        @DisplayName("GIVEN null or blank If-Match WHEN requireMatch THEN throws IfMatchRequiredException")
        void givenNullOrBlankIfMatch_whenRequireMatch_thenThrowsIfMatchRequiredException() {
            Instant now = Instant.now();

            assertThatThrownBy(() -> ConcurrencyGuard.requireMatch(null, now, null))
                .isInstanceOf(IfMatchRequiredException.class);

            assertThatThrownBy(() -> ConcurrencyGuard.requireMatch("   ", now, null))
                .isInstanceOf(IfMatchRequiredException.class);
        }

        @Test
        @DisplayName("GIVEN malformed If-Match WHEN requireMatch THEN throws IfMatchRequiredException")
        void givenMalformedIfMatch_whenRequireMatch_thenThrowsIfMatchRequiredException() {
            Instant now = Instant.now();

            assertThatThrownBy(() -> ConcurrencyGuard.requireMatch("invalid-date-format", now, null))
                .isInstanceOf(IfMatchRequiredException.class);
        }

        @Test
        @DisplayName("GIVEN null currentUpdatedAt WHEN requireMatch THEN succeeds safely")
        void givenNullCurrentUpdatedAt_whenRequireMatch_thenSucceeds() {
            assertThatCode(() -> ConcurrencyGuard.requireMatch("2026-11-10T10:00:00Z", null, null))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("RequestContextHolder Auto-Resolution Tests")
    class RequestContextResolutionTests {

        @Test
        @DisplayName("GIVEN HTTP request with matching If-Match header WHEN requireMatch THEN succeeds")
        void givenHttpRequestWithMatchingHeader_whenRequireMatch_thenSucceeds() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(HttpHeaders.IF_MATCH, "\"" + now.toString() + "\"");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            assertThatCode(() -> ConcurrencyGuard.requireMatch(now))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("GIVEN HTTP request without If-Match header WHEN requireMatch THEN throws IfMatchRequiredException")
        void givenHttpRequestWithoutHeader_whenRequireMatch_thenThrowsIfMatchRequiredException() {
            Instant now = Instant.now();
            MockHttpServletRequest request = new MockHttpServletRequest();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            assertThatThrownBy(() -> ConcurrencyGuard.requireMatch(now))
                .isInstanceOf(IfMatchRequiredException.class);
        }

        @Test
        @DisplayName("GIVEN no RequestContextHolder attributes WHEN requireMatch THEN throws IfMatchRequiredException")
        void givenNoRequestContext_whenRequireMatch_thenThrowsIfMatchRequiredException() {
            Instant now = Instant.now();

            assertThatThrownBy(() -> ConcurrencyGuard.requireMatch(now))
                .isInstanceOf(IfMatchRequiredException.class);
        }
    }
}
