package org.example.onlinemart.service.impl;


import org.example.onlinemart.cache.CacheKeys;
import org.example.onlinemart.cache.CacheService;

import org.example.onlinemart.dao.OrderDAO;
import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.dao.ProductDAO;
import org.example.onlinemart.dao.UserDAO;
import org.example.onlinemart.entity.Order;
import org.example.onlinemart.entity.OrderItem;
import org.example.onlinemart.entity.Product;
import org.example.onlinemart.entity.User;
import org.example.onlinemart.entity.Order.OrderStatus;
import org.example.onlinemart.exception.CacheException;
import org.example.onlinemart.exception.NotEnoughInventoryException;
import org.example.onlinemart.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.hibernate.Hibernate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static sun.plugin2.util.PojoUtil.toJson;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderDAO orderDAO;
    private final OrderItemDAO orderItemDAO;
    private final ProductDAO productDAO;
    private final UserDAO userDAO;

    private final CacheService cacheService;

    @Value("${redis.cache.orders.TTL:60}")
    private long orderCacheTTL;

    @Autowired
    public OrderServiceImpl(OrderDAO orderDAO, OrderItemDAO orderItemDAO,
                            ProductDAO productDAO, UserDAO userDAO,
                            CacheService cacheService) {
        this.orderDAO = orderDAO;
        this.orderItemDAO = orderItemDAO;
        this.productDAO = productDAO;
        this.userDAO = userDAO;

        this.cacheService = cacheService;

    }

    @Override
    public Order createOrder(int userId, List<OrderItem> items) {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderStatus(OrderStatus.Processing);
        order.setOrderTime(new Date());
        orderDAO.save(order);

        for (OrderItem oi : items) {
            Product product = productDAO.findById(oi.getProduct().getProductId());
            if (product == null) {
                throw new RuntimeException("Product not found");
            }
            int requestedQty = oi.getQuantity();
            if (requestedQty > product.getStock()) {
                throw new NotEnoughInventoryException("Not enough inventory for product ID: "
                        + product.getProductId());
            }

            product.setStock(product.getStock() - requestedQty);
            productDAO.update(product);

            oi.setOrder(order);
            oi.setProduct(product);
            oi.setWholesalePriceSnapshot(product.getWholesalePrice());
            oi.setRetailPriceSnapshot(product.getRetailPrice());
            orderItemDAO.save(oi);
        }

        invalidateOrderCaches(userId);

        return order;
    }

    @Override
    public Order cancelOrder(int orderId) {
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }
        if (order.getOrderStatus() == OrderStatus.Completed) {
            throw new RuntimeException("Cannot cancel a completed order");
        }
        if (order.getOrderStatus() == OrderStatus.Canceled) {

            if (order.getUser() != null) {
                Hibernate.initialize(order.getUser());
            }
            return order;
        }

        List<OrderItem> items = orderItemDAO.findByOrderId(orderId);
        for (OrderItem oi : items) {
            Product product = oi.getProduct();
            product.setStock(product.getStock() + oi.getQuantity());
            productDAO.update(product);
        }

        order.setOrderStatus(OrderStatus.Canceled);
        orderDAO.update(order);

        if (order.getUser() != null) {
            Hibernate.initialize(order.getUser());
        }


        invalidateOrderCaches(order.getUser().getUserId());
        cacheService.delete(CacheKeys.Orders.order(orderId));


        return order;
    }

    @Override
    public Order completeOrder(int orderId) {
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }
        if (order.getOrderStatus() == OrderStatus.Canceled) {
            throw new RuntimeException("Cannot complete a canceled order");
        }
        if (order.getOrderStatus() == OrderStatus.Completed) {
            if (order.getUser() != null) {
                Hibernate.initialize(order.getUser());
            }
            return order;
        }

        order.setOrderStatus(OrderStatus.Completed);
        orderDAO.update(order);

        if (order.getUser() != null) {
            Hibernate.initialize(order.getUser());
        }


        invalidateOrderCaches(order.getUser().getUserId());
        cacheService.delete(CacheKeys.Orders.order(orderId));

        cacheService.delete(CacheKeys.AdminSummary.MOST_PROFITABLE);
        cacheService.delete(CacheKeys.AdminSummary.TOTAL_SOLD);
        cacheService.delete(CacheKeys.AdminSummary.topPopular(3));
        return order;
    }

    @Override
    public Order findById(int orderId) {

        String cacheKey = CacheKeys.Orders.order(orderId);
        Optional<Order> cachedOrder = cacheService.get(cacheKey, Order.class);

        if (cachedOrder.isPresent()) {
            logger.debug("Cache hit for order ID: {}", orderId);
            return cachedOrder.get();
        }

        logger.debug("Cache miss for order ID: {}", orderId);
        Order order = orderDAO.findById(orderId);

        if (order != null) {
            if (order.getUser() != null) {
                Hibernate.initialize(order.getUser());
            }

            cacheService.set(cacheKey, order, orderCacheTTL, TimeUnit.SECONDS);
        }

        return order;
    }

    @Override
    public List<Order> findAllCached() {
        String cacheKey = CacheKeys.Orders.ALL;
        Optional<List<Order>> cachedOrders = cacheService.getList(cacheKey, Order.class);

        if (cachedOrders.isPresent()) {
            logger.debug("Cache hit for all orders");
            return cachedOrders.get();
        }

        logger.debug("Cache miss for all orders");
        List<Order> result = orderDAO.findAll();

        cacheService.set(cacheKey, result, orderCacheTTL, TimeUnit.SECONDS);

        return result;
    }

    private <T> List<T> fromJsonList(String json, Class<T> clazz) {
        Type typeOfT = TypeToken.getParameterized(List.class, clazz).getType();
        return new Gson().fromJson(json, typeOfT);
    }

    public List<Order> findAllCached() {
        String cacheKey = "orders:all";
        String cachedJson = jedis.get(cacheKey);
        if (cachedJson != null) {
            return fromJsonList(cachedJson, Order.class);
        }
        List<Order> result = orderDAO.findAll();
        jedis.setex(cacheKey, 30, toJson(result));
        return result;
    }


    @Override
    public List<Order> findAll() {
        return orderDAO.findAll();
    }

    @Override
    public List<Order> findByUserId(int userId) {
        String cacheKey = CacheKeys.Orders.userOrders(userId);
        Optional<List<Order>> cachedOrders = cacheService.getList(cacheKey, Order.class);

        if (cachedOrders.isPresent()) {
            logger.debug("Cache hit for user orders: {}", userId);
            return cachedOrders.get();
        }

        logger.debug("Cache miss for user orders: {}", userId);
        List<Order> orders = orderDAO.findByUserId(userId);

        for (Order o : orders) {
            if (o.getUser() != null) {
                Hibernate.initialize(o.getUser());
            }
        }

        cacheService.set(cacheKey, orders, orderCacheTTL, TimeUnit.SECONDS);

        return orders;
    }

    @Override
    public List<Order> findAllPaginated(int offset, int limit) {

        String cacheKey = CacheKeys.Orders.paginated(offset / limit + 1, limit);
        Optional<List<Order>> cachedOrders = cacheService.getList(cacheKey, Order.class);

        if (cachedOrders.isPresent()) {
            logger.debug("Cache hit for paginated orders: offset={}, limit={}", offset, limit);
            return cachedOrders.get();
        }

        logger.debug("Cache miss for paginated orders: offset={}, limit={}", offset, limit);
        List<Order> result = orderDAO.findAllPaginated(offset, limit);
        cacheService.set(cacheKey, result, orderCacheTTL / 2, TimeUnit.SECONDS);

        return result;
    }

    private void invalidateOrderCaches(int userId) {
        try {
            cacheService.delete(CacheKeys.Orders.ALL);
            cacheService.delete(CacheKeys.Orders.userOrders(userId));

            cacheService.delete(CacheKeys.UserActivity.frequentPurchases(userId, 3));
            cacheService.delete(CacheKeys.UserActivity.recentPurchases(userId, 3));

            for (int page = 1; page <= 3; page++) {
                cacheService.delete(CacheKeys.Orders.paginated(page, 5));
            }
        } catch (CacheException e) {
            logger.warn("Failed to invalidate order caches for user {}", userId, e);
        }
    }
}