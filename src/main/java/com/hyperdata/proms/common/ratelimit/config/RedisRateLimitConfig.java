package com.hyperdata.proms.common.ratelimit.config;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RedisRateLimitConfig {

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public LettuceBasedProxyManager<String> bucketProxyManager(RedisConnectionFactory connectionFactory) {
        if (connectionFactory instanceof LettuceConnectionFactory lettuceConnectionFactory) {
            lettuceConnectionFactory.start();
            RedisClient nativeClient = (RedisClient) lettuceConnectionFactory.getNativeClient();
            StatefulRedisConnection<String, byte[]> connection = nativeClient.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

            return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
                    .basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)))
                .build();
        }
        throw new IllegalStateException("RedisConnectionFactory must be an instance of LettuceConnectionFactory");
    }
}
