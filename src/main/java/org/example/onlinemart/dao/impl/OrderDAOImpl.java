package org.example.onlinemart.dao.impl;

import org.example.onlinemart.cache.CacheKeys;
import org.example.onlinemart.cache.CacheService;
import org.example.onlinemart.dao.OrderDAO;
import org.example.onlinemart.entity.Order;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.List;

@Repository
@Transactional
public class OrderDAOImpl implements OrderDAO {

    private static final Logger logger = LoggerFactory.getLogger(OrderDAOImpl.class);

    private final SessionFactory sessionFactory;
    private final CacheService cacheService;

    @Autowired
    public OrderDAOImpl(SessionFactory sessionFactory, CacheService cacheService) {
        this.sessionFactory = sessionFactory;
        this.cacheService = cacheService;
    }

    @Override
    public void save(Order order) {
        sessionFactory.getCurrentSession().save(order);

        // No need to invalidate caches here as this is a new order
        // and won't affect existing cached data
    }

    @Override
    public void update(Order order) {
        sessionFactory.getCurrentSession().update(order);


        // Invalidate caches related to this order
        try {
            invalidateOrderCaches(order);
        } catch (Exception e) {
            logger.warn("Failed to invalidate order caches for orderID {}", order.getOrderId(), e);
            // Don't propagate the exception - data is updated, cache invalidation is secondary
        }

    }

    @Override
    public Order findById(int orderId) {
        return sessionFactory.getCurrentSession().get(Order.class, orderId);
    }

    @Override
    public List<Order> findAll() {
        Session session = sessionFactory.getCurrentSession();
        Query<Order> query = session.createQuery(
                "SELECT DISTINCT o FROM Order o JOIN FETCH o.user",
                Order.class
        );
        return query.list();
    }

    @Override
    public List<Order> findByUserId(int userId) {
        Session session = sessionFactory.getCurrentSession();
        String hql = "FROM Order o WHERE o.user.userId = :uid";
        Query<Order> query = session.createQuery(hql, Order.class);
        query.setParameter("uid", userId);
        return query.list();
    }

    @Override
    public List<Order> findAllPaginated(int offset, int limit) {
        Session session = sessionFactory.getCurrentSession();
        Query<Order> query = session.createQuery(
                "SELECT DISTINCT o " +
                        "FROM Order o " +
                        "JOIN FETCH o.user " +
                        "ORDER BY o.orderTime DESC",
                Order.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }

    /**
     * Helper method to invalidate all caches related to an order
     *
     * @param order The order whose caches should be invalidated
     */
    private void invalidateOrderCaches(Order order) {
        // Always invalidate the specific order cache
        cacheService.delete(CacheKeys.Orders.order(order.getOrderId()));

        // Always invalidate the "all orders" cache
        cacheService.delete(CacheKeys.Orders.ALL);

        // If the order has a user, invalidate user-specific order cache
        if (order.getUser() != null) {
            int userId = order.getUser().getUserId();
            cacheService.delete(CacheKeys.Orders.userOrders(userId));

            // Also invalidate user activity caches that depend on orders
            cacheService.delete(CacheKeys.UserActivity.frequentPurchases(userId, 3));
            cacheService.delete(CacheKeys.UserActivity.recentPurchases(userId, 3));
        }

        // Invalidate the first few pages of paginated results
        // This is a simple approach; a more sophisticated approach would track which pages
        // might contain this order, but this ensures data consistency for the most commonly
        // accessed pages
        for (int page = 1; page <= 3; page++) {
            cacheService.delete(CacheKeys.Orders.paginated(page, 5));
        }

        // If order status changed to completed, invalidate admin summary caches
        if (order.getOrderStatus() == Order.OrderStatus.Completed) {
            cacheService.delete(CacheKeys.AdminSummary.MOST_PROFITABLE);
            cacheService.delete(CacheKeys.AdminSummary.TOTAL_SOLD);
            cacheService.delete(CacheKeys.AdminSummary.topPopular(3));
        }

        logger.debug("Invalidated caches for order ID: {}", order.getOrderId());
    }
}
