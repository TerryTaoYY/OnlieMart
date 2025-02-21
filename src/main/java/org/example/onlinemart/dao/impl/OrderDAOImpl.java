package org.example.onlinemart.dao.impl;

import org.example.onlinemart.dao.OrderDAO;
import org.example.onlinemart.entity.Order;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class OrderDAOImpl implements OrderDAO {

    private final SessionFactory sessionFactory;

    public OrderDAOImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(Order order) {
        sessionFactory.getCurrentSession().save(order);
    }

    @Override
    public void update(Order order) {
        sessionFactory.getCurrentSession().update(order);
    }

    @Override
    public Order findById(int orderId) {
        return sessionFactory.getCurrentSession().get(Order.class, orderId);
    }

    @Override
    public List<Order> findAll() {
        Session session = sessionFactory.getCurrentSession();
        return session.createQuery("FROM Order", Order.class).list();
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
        // Example sorting by orderTime descending; adjust if you prefer a different sorting
        Query<Order> query = session.createQuery("FROM Order o ORDER BY o.orderTime DESC", Order.class);
        query.setFirstResult(offset);
        query.setMaxResults(limit);
        return query.list();
    }
}
