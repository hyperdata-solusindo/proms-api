package com.hyperdata.proms.common.ratelimit.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RedisRateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration redisTimeout;

    @Bean
    @ConditionalOnMissingBean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean(destroyMethod = "shutdown")
    public RedisClient bucketRedisClient() {
        RedisURI.Builder builder = RedisURI.builder()
            .withHost(redisHost)
            .withPort(redisPort)
            .withTimeout(redisTimeout);

        if (redisPassword != null && !redisPassword.isBlank()) {
            builder.withPassword(redisPassword.toCharArray());
        }

        return RedisClient.create(builder.build());
    }

    @Bean(destroyMethod = "close")
    public ProxyManager<String> bucketProxyManager(RedisClient bucketRedisClient) {
        return new ResilientLettuceProxyManager(
            bucketRedisClient,
            Duration.ofMinutes(10),
            redisHost,
            redisPort);
    }
}
