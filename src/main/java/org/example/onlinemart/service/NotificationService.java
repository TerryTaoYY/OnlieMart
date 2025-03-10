package org.example.onlinemart.service;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Service for real-time notifications using Redis Pub/Sub.
 * Enables broadcasting events to multiple application instances.
 */
@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final JedisPool jedisPool;
    private final Gson gson = new Gson();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, JedisPubSub> subscribers = new ConcurrentHashMap<>();
    private final Map<String, Consumer<Map<String, Object>>> handlers = new ConcurrentHashMap<>();

    @Autowired
    public NotificationService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @PostConstruct
    public void init() {
        // Register default channels at startup
        subscribe("order-status-change", this::handleOrderStatusChange);
        subscribe("inventory-update", this::handleInventoryUpdate);
        subscribe("user-activity", this::handleUserActivity);
    }

    @PreDestroy
    public void cleanup() {
        // Unsubscribe from all channels and shutdown executor
        subscribers.forEach((channel, subscriber) -> subscriber.unsubscribe());
        executorService.shutdown();
    }

    /**
     * Subscribe to a notification channel with a custom handler.
     *
     * @param channel The channel name to subscribe to
     * @param handler The handler function to process notifications
     */
    public void subscribe(String channel, Consumer<Map<String, Object>> handler) {
        if (subscribers.containsKey(channel)) {
            logger.info("Already subscribed to channel: {}", channel);
            return;
        }

        handlers.put(channel, handler);
        JedisPubSub subscriber = createSubscriber();
        subscribers.put(channel, subscriber);

        executorService.submit(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                logger.info("Subscribing to channel: {}", channel);
                jedis.subscribe(subscriber, channel);
            } catch (Exception e) {
                logger.error("Error in subscription to channel: {}", channel, e);
            }
        });
    }

    /**
     * Unsubscribe from a notification channel.
     *
     * @param channel The channel name to unsubscribe from
     */
    public void unsubscribe(String channel) {
        JedisPubSub subscriber = subscribers.get(channel);
        if (subscriber != null) {
            subscriber.unsubscribe(channel);
            subscribers.remove(channel);
            handlers.remove(channel);
            logger.info("Unsubscribed from channel: {}", channel);
        }
    }

    /**
     * Publish a notification to a channel.
     *
     * @param channel The channel to publish to
     * @param data The data to publish
     */
    public void publish(String channel, Map<String, Object> data) {
        try (Jedis jedis = jedisPool.getResource()) {
            String message = gson.toJson(data);
            jedis.publish(channel, message);
            logger.debug("Published to channel: {}, message: {}", channel, message);
        } catch (Exception e) {
            logger.error("Error publishing to channel: {}", channel, e);
        }
    }

    /**
     * Create a JedisPubSub instance to handle incoming messages
     */
    private JedisPubSub createSubscriber() {
        return new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = gson.fromJson(message, HashMap.class);
                    logger.debug("Received on channel: {}, message: {}", channel, message);

                    Consumer<Map<String, Object>> handler = handlers.get(channel);
                    if (handler != null) {
                        handler.accept(data);
                    }
                } catch (Exception e) {
                    logger.error("Error processing message on channel: {}", channel, e);
                }
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                logger.info("Subscribed to channel: {}, total subscriptions: {}",
                        channel, subscribedChannels);
            }

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                logger.info("Unsubscribed from channel: {}, remaining subscriptions: {}",
                        channel, subscribedChannels);
            }
        };
    }

    /**
     * Handle order status change notifications
     */
    private void handleOrderStatusChange(Map<String, Object> data) {
        // Implementation would dispatch to appropriate services
        logger.info("Order status changed for order ID: {}, new status: {}",
                data.get("orderId"), data.get("status"));
    }

    /**
     * Handle inventory update notifications
     */
    private void handleInventoryUpdate(Map<String, Object> data) {
        // Implementation would update local caches or trigger UI updates
        logger.info("Inventory updated for product ID: {}, new stock: {}",
                data.get("productId"), data.get("stock"));
    }

    /**
     * Handle user activity notifications
     */
    private void handleUserActivity(Map<String, Object> data) {
        // Implementation would update analytics or activity monitoring
        logger.info("User activity recorded: user ID: {}, action: {}",
                data.get("userId"), data.get("action"));
    }

    /**
     * Send an order status change notification
     */
    public void notifyOrderStatusChange(int orderId, String status, int userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("status", status);
        data.put("userId", userId);
        data.put("timestamp", System.currentTimeMillis());

        publish("order-status-change", data);
    }

    /**
     * Send an inventory update notification
     */
    public void notifyInventoryUpdate(int productId, int stock, String productName) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("stock", stock);
        data.put("productName", productName);
        data.put("timestamp", System.currentTimeMillis());

        publish("inventory-update", data);
    }

    /**
     * Send a user activity notification
     */
    public void notifyUserActivity(int userId, String action, Map<String, Object> details) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("action", action);
        data.put("details", details);
        data.put("timestamp", System.currentTimeMillis());

        publish("user-activity", data);
    }
}