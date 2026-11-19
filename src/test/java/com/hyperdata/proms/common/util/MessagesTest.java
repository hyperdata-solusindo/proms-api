package com.hyperdata.proms.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.hyperdata.proms.common.config.LocalizationConfig;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

class MessagesTest {

    @BeforeEach
    void setUp() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(LocalizationConfig.resolveBasenames());
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(Locale.of("id", "ID"));
        new Messages(messageSource);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("GIVEN Indonesian locale WHEN Messages.get THEN returns Indonesian message")
    void givenIndonesianLocale_whenGet_thenReturnsIndonesian() {
        LocaleContextHolder.setLocale(Locale.of("id", "ID"));
        assertThat(Messages.get("employee.created")).isEqualTo("Karyawan berhasil ditambahkan");
        assertThat(Messages.get("auth.login.success")).isEqualTo("Berhasil login");
    }

    @Test
    @DisplayName("GIVEN English locale WHEN Messages.get THEN returns English message")
    void givenEnglishLocale_whenGet_thenReturnsEnglish() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(Messages.get("employee.created")).isEqualTo("Employee created successfully");
        assertThat(Messages.get("auth.login.success")).isEqualTo("Login successful");
    }

    @Test
    @DisplayName("GIVEN unknown message code WHEN Messages.get THEN falls back to code")
    void givenUnknownCode_whenGet_thenReturnsCode() {
        assertThat(Messages.get("unknown.code.xyz")).isEqualTo("unknown.code.xyz");
    }
}
