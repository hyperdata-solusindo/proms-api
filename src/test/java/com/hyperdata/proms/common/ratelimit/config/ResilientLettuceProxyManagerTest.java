package com.hyperdata.proms.common.ratelimit.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResilientLettuceProxyManagerTest {

    @Test
    @DisplayName("Should not throw exception on startup when Redis is unreachable")
    void shouldNotThrowOnStartupWhenRedisUnreachable() {
        RedisClient redisClient = mock(RedisClient.class);
        when(redisClient.connect(any(RedisCodec.class)))
            .thenThrow(new RedisConnectionException("Connection refused"));

        ResilientLettuceProxyManager manager = new ResilientLettuceProxyManager(
            redisClient,
            Duration.ofMinutes(10),
            "localhost",
            6379);

        assertThat(manager).isNotNull();
        assertThat(manager.isAsyncModeSupported()).isFalse();
        assertThat(manager.isExpireAfterWriteSupported()).isFalse();

        assertThatThrownBy(manager::builder)
            .isInstanceOf(RedisException.class)
            .hasMessageContaining("Redis is unreachable");
    }

    @Test
    @DisplayName("Should successfully connect and delegate when Redis is available")
    @SuppressWarnings("unchecked")
    void shouldDelegateWhenRedisIsAvailable() {
        RedisClient redisClient = mock(RedisClient.class);
        StatefulRedisConnection<String, byte[]> connection = mock(StatefulRedisConnection.class);
        RedisAsyncCommands<String, byte[]> asyncCommands = mock(RedisAsyncCommands.class);

        when(connection.isOpen()).thenReturn(true);
        when(connection.async()).thenReturn(asyncCommands);
        doReturn(connection).when(redisClient).connect(any(RedisCodec.class));

        ResilientLettuceProxyManager manager = new ResilientLettuceProxyManager(
            redisClient,
            Duration.ofMinutes(10),
            "localhost",
            6379);

        assertThat(manager.isAsyncModeSupported()).isTrue();
        assertThat(manager.isExpireAfterWriteSupported()).isTrue();
        assertThat(manager.builder()).isNotNull();

        manager.close();
        verify(connection).close();
    }
}
