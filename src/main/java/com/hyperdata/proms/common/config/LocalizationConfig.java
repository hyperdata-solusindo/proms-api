package com.hyperdata.proms.common.config;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class LocalizationConfig {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LocalizationConfig.class);

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.of("id", "ID"));
        return resolver;
    }

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames(resolveBasenames());
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(Locale.of("id", "ID"));
        return messageSource;
    }

    public static String[] resolveBasenames() {
        try {
            org.springframework.core.io.support.ResourcePatternResolver resolver = new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
            org.springframework.core.io.Resource[] resources = resolver
                .getResources("classpath*:messages/**/*.properties");
            java.util.Set<String> basenames = new java.util.TreeSet<>();
            for (org.springframework.core.io.Resource resource : resources) {
                String path = resource.getURL().toString();
                int idx = path.indexOf("messages/");
                if (idx != -1) {
                    String sub = path.substring(idx);
                    String clean = sub.replaceAll("(_[a-z]{2}(_[A-Z]{2})?)?\\.properties$", "");
                    basenames.add("classpath:" + clean);
                }
            }
            if (!basenames.isEmpty()) {
                log.info("Discovered {} message basenames", basenames.size());
                return basenames.toArray(String[]::new);
            }
        } catch (java.io.IOException ex) {
            log.warn("Failed scanning message bundles, using fallback list: {}", ex.getMessage());
        }

        return new String[]{
            "classpath:messages/common/common",
            "classpath:messages/identity/auth",
            "classpath:messages/identity/user",
            "classpath:messages/sample/employee",
            "classpath:messages/sample/product",
            "classpath:messages/proms/board"
        };
    }
}
