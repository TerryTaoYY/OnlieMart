package org.example.onlinemart.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.onlinemart.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis implementation of the CacheService interface.
 */
@Service
public class RedisCacheService implements CacheService {

    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);

    private final JedisPool jedisPool;
    private final Gson gson;

    @Value("${redis.cache.defaultTTL:300}")
    private long defaultTTL;  // Default time-to-live in seconds

    @Autowired
    public RedisCacheService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").create();
    }

    @Override
    public <T> boolean set(String key, T value) {
        return set(key, value, defaultTTL, TimeUnit.SECONDS);
    }

    @Override
    public <T> boolean set(String key, T value, long expiration, TimeUnit timeUnit) {
        if (key == null || value == null) {
            return false;
        }

        String json = gson.toJson(value);
        long seconds = timeUnit.toSeconds(expiration);

        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.setex(key, seconds, json);
            return "OK".equals(result);
        } catch (JedisException e) {
            logger.error("Error setting key {} in cache", key, e);
            handleJedisException(e);
            return false;
        }
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> clazz) {
        if (key == null || clazz == null) {
            return Optional.empty();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(key);
            if (json == null) {
                return Optional.empty();
            }

            T value = gson.fromJson(json, clazz);
            return Optional.ofNullable(value);
        } catch (JedisException e) {
            logger.error("Error getting key {} from cache", key, e);
            handleJedisException(e);
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<List<T>> getList(String key, Class<T> clazz) {
        if (key == null || clazz == null) {
            return Optional.empty();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get(key);
            if (json == null) {
                return Optional.empty();
            }

            Type listType = TypeToken.getParameterized(List.class, clazz).getType();
            List<T> list = gson.fromJson(json, listType);
            return Optional.ofNullable(list);
        } catch (JedisException e) {
            logger.error("Error getting list key {} from cache", key, e);
            handleJedisException(e);
            return Optional.empty();
        }
    }

    @Override
    public boolean exists(String key) {
        if (key == null) {
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        } catch (JedisException e) {
            logger.error("Error checking existence of key {} in cache", key, e);
            handleJedisException(e);
            return false;
        }
    }

    @Override
    public boolean delete(String key) {
        if (key == null) {
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.del(key) > 0;
        } catch (JedisException e) {
            logger.error("Error deleting key {} from cache", key, e);
            handleJedisException(e);
            return false;
        }
    }

    @Override
    public long delete(String... keys) {
        if (keys == null || keys.length == 0) {
            return 0;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.del(keys);
        } catch (JedisException e) {
            logger.error("Error deleting multiple keys from cache", e);
            handleJedisException(e);
            return 0;
        }
    }

    @Override
    public boolean expire(String key, long expiration, TimeUnit timeUnit) {
        if (key == null) {
            return false;
        }

        long seconds = timeUnit.toSeconds(expiration);

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.expire(key, seconds) == 1;
        } catch (JedisException e) {
            logger.error("Error setting expiration for key {} in cache", key, e);
            handleJedisException(e);
            return false;
        }
    }

    @Override
    public <T> boolean hashSet(String key, String field, T value) {
        if (key == null || field == null || value == null) {
            return false;
        }

        String json = gson.toJson(value);

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hset(key, field, json) == 1;
        } catch (JedisException e) {
            logger.error("Error setting hash field {} for key {} in cache", field, key, e);
            handleJedisException(e);
            return false;
        }
    }

    @Override
    public <T> Optional<T> hashGet(String key, String field, Class<T> clazz) {
        if (key == null || field == null || clazz == null) {
            return Optional.empty();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.hget(key, field);
            if (json == null) {
                return Optional.empty();
            }

            T value = gson.fromJson(json, clazz);
            return Optional.ofNullable(value);
        } catch (JedisException e) {
            logger.error("Error getting hash field {} for key {} from cache", field, key, e);
            handleJedisException(e);
            return Optional.empty();
        }
    }

    @Override
    public <T> Map<String, T> hashGetAll(String key, Class<T> clazz) {
        if (key == null || clazz == null) {
            return Collections.emptyMap();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            Map<String, String> rawMap = jedis.hgetAll(key);
            if (rawMap.isEmpty()) {
                return Collections.emptyMap();
            }

            Map<String, T> result = new HashMap<>(rawMap.size());
            for (Map.Entry<String, String> entry : rawMap.entrySet()) {
                T value = gson.fromJson(entry.getValue(), clazz);
                result.put(entry.getKey(), value);
            }

            return result;
        } catch (JedisException e) {
            logger.error("Error getting all hash fields for key {} from cache", key, e);
            handleJedisException(e);
            return Collections.emptyMap();
        }
    }

    @Override
    public boolean hashDelete(String key, String field) {
        if (key == null || field == null) {
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hdel(key, field) > 0;
        } catch (JedisException e) {
            logger.error("Error deleting hash field {} for key {} from cache", field, key, e);
            handleJedisException(e);
            return false;
        }
    }

    @Override
    public long increment(String key) {
        return incrementBy(key, 1);
    }

    @Override
    public long incrementBy(String key, long amount) {
        if (key == null) {
            return 0;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incrBy(key, amount);
        } catch (JedisException e) {
            logger.error("Error incrementing key {} in cache", key, e);
            handleJedisException(e);
            return 0;
        }
    }

    /**
     * Handle JedisException - this method can be extended to implement circuit breaker
     * or other fault tolerance patterns.
     *
     * @param e The Jedis exception
     */
    private void handleJedisException(JedisException e) {
        // For now, just wrap in a custom exception
        throw new CacheException("Redis cache operation failed", e);
    }
}