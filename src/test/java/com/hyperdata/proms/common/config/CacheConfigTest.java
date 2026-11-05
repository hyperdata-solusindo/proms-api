package com.hyperdata.proms.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

class CacheConfigTest {

    private final CacheConfig cacheConfig = new CacheConfig();

    @Test
    @DisplayName("Given cache configuration, when cacheManager bean is created, then returns CaffeineCacheManager")
    void givenCacheConfig_whenCacheManagerCreated_thenReturnsCaffeineCacheManager() {
        CacheManager cacheManager = cacheConfig.cacheManager(10, 1000);

        assertThat(cacheManager).isInstanceOf(CaffeineCacheManager.class);
    }

    @Test
    @DisplayName("Given active cache manager, when caching value, then can retrieve cached entry successfully")
    void givenActiveCacheManager_whenCachingValue_thenCanRetrieveCachedEntry() {
        CacheManager cacheManager = cacheConfig.cacheManager(10, 1000);
        Cache cache = cacheManager.getCache("sample-cache");

        assertThat(cache).isNotNull();
        cache.put("tenant-key-1", "tenant-data");

        Cache.ValueWrapper wrapper = cache.get("tenant-key-1");
        assertThat(wrapper).isNotNull();
        assertThat(wrapper.get()).isEqualTo("tenant-data");
    }
}
