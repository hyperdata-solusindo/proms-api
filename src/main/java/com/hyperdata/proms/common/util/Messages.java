package com.hyperdata.proms.common.util;

import com.hyperdata.proms.common.config.LocalizationConfig;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Utility helper untuk mengambil pesan terlokalisasi berdasarkan Locale konteks request aktif.
 */
@Component
public class Messages {

    public static final Locale DEFAULT_LOCALE = Locale.of("id", "ID");

    private static volatile MessageSource messageSource;

    static {
        ReloadableResourceBundleMessageSource defaultSource = new ReloadableResourceBundleMessageSource();
        defaultSource.setBasenames(LocalizationConfig.resolveBasenames());
        defaultSource.setDefaultEncoding("UTF-8");
        defaultSource.setDefaultLocale(DEFAULT_LOCALE);
        defaultSource.setFallbackToSystemLocale(false);
        messageSource = defaultSource;
    }

    public Messages(MessageSource messageSource) {
        Messages.messageSource = messageSource;
    }

    /**
     * Mengambil pesan terjemahan berdasarkan message code dan LocaleContextHolder / Accept-Language header.
     * Jika tidak ditemukan, mengembalikan message code itu sendiri sebagai fallback.
     */
    public static String get(String code, Object... args) {
        Locale locale = resolveLocale();
        return messageSource.getMessage(code, args, code, locale);
    }

    public static Locale resolveLocale() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            String acceptLanguage = servletAttributes.getRequest().getHeader("Accept-Language");
            if (acceptLanguage != null) {
                if (acceptLanguage.toLowerCase().startsWith("en")) {
                    return Locale.ENGLISH;
                }
                return DEFAULT_LOCALE;
            }
            return DEFAULT_LOCALE;
        }

        LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
        if (localeContext != null && localeContext.getLocale() != null) {
            if (localeContext.getLocale().getLanguage().equalsIgnoreCase("en")) {
                return Locale.ENGLISH;
            }
        }
        return DEFAULT_LOCALE;
    }
}
