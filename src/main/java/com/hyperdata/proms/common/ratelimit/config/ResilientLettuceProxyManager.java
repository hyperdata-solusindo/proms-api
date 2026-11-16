package com.hyperdata.proms.common.ratelimit.config;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resilient Lettuce ProxyManager that does not crash application startup if Redis is
 * temporarily unreachable or initializing. Automatically establishes connection on demand
 * and falls back to fail-open exception handling when Redis is offline.
 */
public class ResilientLettuceProxyManager implements ProxyManager<String>, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ResilientLettuceProxyManager.class);

    private final RedisClient redisClient;
    private final Duration expirationDuration;
    private final String redisHost;
    private final int redisPort;
    private volatile LettuceBasedProxyManager<String> delegate;
    private volatile StatefulRedisConnection<String, byte[]> connection;

    public ResilientLettuceProxyManager(
        RedisClient redisClient,
        Duration expirationDuration,
        String redisHost,
        int redisPort) {
        this.redisClient = redisClient;
        this.expirationDuration = expirationDuration;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        tryConnect();
    }

    private synchronized void tryConnect() {
        if (delegate != null && connection != null && connection.isOpen()) {
            return;
        }
        try {
            this.connection = redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
            this.delegate = LettuceBasedProxyManager.builderFor(this.connection)
                .withExpirationStrategy(ExpirationAfterWriteStrategy
                    .basedOnTimeForRefillingBucketUpToMax(expirationDuration))
                .build();
            log.info("Connected to Redis at {}:{} for distributed rate limiting.", redisHost, redisPort);
        } catch (Exception e) {
            log.warn("Redis is not currently reachable at {}:{} ({}). Rate limiting will fail-open gracefully.",
                redisHost, redisPort, e.getMessage());
            this.delegate = null;
            if (this.connection != null) {
                try {
                    this.connection.close();
                } catch (Exception ignored) {
                }
                this.connection = null;
            }
        }
    }

    private ProxyManager<String> getDelegateOrConnect() {
        if (delegate == null || connection == null || !connection.isOpen()) {
            tryConnect();
        }
        if (delegate != null) {
            return delegate;
        }
        throw new RedisException("Redis is unreachable at " + redisHost + ":" + redisPort);
    }

    @Override
    public RemoteBucketBuilder<String> builder() {
        return getDelegateOrConnect().builder();
    }

    @Override
    public Optional<BucketConfiguration> getProxyConfiguration(String key) {
        return getDelegateOrConnect().getProxyConfiguration(key);
    }

    @Override
    public void removeProxy(String key) {
        getDelegateOrConnect().removeProxy(key);
    }

    @Override
    public boolean isAsyncModeSupported() {
        ProxyManager<String> activeDelegate = delegate;
        return activeDelegate != null && activeDelegate.isAsyncModeSupported();
    }

    @Override
    public boolean isExpireAfterWriteSupported() {
        ProxyManager<String> activeDelegate = delegate;
        return activeDelegate != null && activeDelegate.isExpireAfterWriteSupported();
    }

    @Override
    public AsyncProxyManager<String> asAsync() {
        return getDelegateOrConnect().asAsync();
    }

    @Override
    public <K1> ProxyManager<K1> withMapper(Function<? super K1, ? extends String> mapper) {
        return getDelegateOrConnect().withMapper(mapper);
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception ignored) {
            }
        }
    }
}
