package org.example.onlinemart.dao.impl;

import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.entity.OrderItem;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class OrderItemDAOImpl implements OrderItemDAO {

    private final SessionFactory sessionFactory;

    public OrderItemDAOImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(OrderItem orderItem) {
        sessionFactory.getCurrentSession().save(orderItem);
    }

    @Override
    public void update(OrderItem orderItem) {
        sessionFactory.getCurrentSession().update(orderItem);
    }

    @Override
    public List<OrderItem> findByOrderId(int orderId) {
        Session session = sessionFactory.getCurrentSession();
        String hql = "FROM OrderItem oi WHERE oi.order.orderId = :oid";
        Query<OrderItem> query = session.createQuery(hql, OrderItem.class);
        query.setParameter("oid", orderId);
        return query.list();
    }
}
