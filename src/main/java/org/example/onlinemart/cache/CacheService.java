package org.example.onlinemart.cache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface CacheService {

    <T> boolean set(String key, T value);

    <T> boolean set(String key, T value, long expiration, TimeUnit timeUnit);

    <T> Optional<T> get(String key, Class<T> clazz);

    <T> Optional<List<T>> getList(String key, Class<T> clazz);

    boolean exists(String key);

    boolean delete(String key);

    long delete(String... keys);

    boolean expire(String key, long expiration, TimeUnit timeUnit);

    <T> boolean hashSet(String key, String field, T value);

    <T> Optional<T> hashGet(String key, String field, Class<T> clazz);

    <T> Map<String, T> hashGetAll(String key, Class<T> clazz);

    boolean hashDelete(String key, String field);

    long increment(String key);

    long incrementBy(String key, long amount);
}