package org.example.onlinemart.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Jedis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Configuration
public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${redis.host:localhost}")
    private String redisHost;

    @Value("${redis.port:6379}")
    private int redisPort;

    @Value("${redis.password:}")
    private String redisPassword;

    @Value("${redis.timeout:2000}")
    private int redisTimeout;

    @Value("${redis.pool.maxTotal:50}")
    private int maxTotal;

    @Value("${redis.pool.maxIdle:20}")
    private int maxIdle;

    @Value("${redis.pool.minIdle:5}")
    private int minIdle;

    @Value("${redis.pool.testOnBorrow:true}")
    private boolean testOnBorrow;

    @Value("${redis.pool.testWhileIdle:true}")
    private boolean testWhileIdle;

    @Value("${redis.pool.maxWaitMillis:3000}")
    private long maxWaitMillis;

    @Bean
    public JedisPoolConfig jedisPoolConfig() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxTotal);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMinIdle(minIdle);
        poolConfig.setTestOnBorrow(testOnBorrow);
        poolConfig.setTestWhileIdle(testWhileIdle);
        poolConfig.setMaxWait(Duration.ofMillis(maxWaitMillis));
        poolConfig.setBlockWhenExhausted(true);

        return poolConfig;
    }

    @Bean
    public JedisPool jedisPool(JedisPoolConfig poolConfig) {
        logger.info("Creating Redis connection pool to {}:{}", redisHost, redisPort);

        if (redisPassword != null && !redisPassword.isEmpty()) {
            return new JedisPool(poolConfig, redisHost, redisPort, redisTimeout, redisPassword);
        } else {
            return new JedisPool(poolConfig, redisHost, redisPort, redisTimeout);
        }
    }

    @Bean
    public Jedis jedisClient(JedisPool jedisPool) {
        // This is needed for backward compatibility with the existing code
        // Ideally, code should be refactored to use JedisPool directly
        return jedisPool.getResource();
    }
}
