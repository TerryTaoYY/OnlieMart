package org.example.onlinemart.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Bean
    public redis.clients.jedis.Jedis jedisClient() {
        return new redis.clients.jedis.Jedis("localhost", 6379);
    }
}
