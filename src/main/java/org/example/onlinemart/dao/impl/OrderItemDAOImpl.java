package org.example.onlinemart.dao.impl;

import org.example.onlinemart.dao.OrderItemDAO;
import org.example.onlinemart.entity.OrderItem;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Repository
@Transactional
public class OrderItemDAOImpl implements OrderItemDAO {

    private final SessionFactory sessionFactory;

    @PersistenceContext
    private EntityManager entityManager;

    public OrderItemDAOImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(OrderItem orderItem) {
        entityManager.persist(orderItem);
    }

    @Override
    public void update(OrderItem orderItem) {
        sessionFactory.getCurrentSession().update(orderItem);
    }

    @Override
    public List<OrderItem> findByOrderId(int orderId) {
        Session session = sessionFactory.getCurrentSession();
        String hql = "SELECT oi " +
                "FROM OrderItem oi " +
                "JOIN FETCH oi.product p " +
                "JOIN FETCH oi.order o " +
                "WHERE o.orderId = :oid";
        Query<OrderItem> query = session.createQuery(hql, OrderItem.class);
        query.setParameter("oid", orderId);
        return query.list();
    }

    @Override
    public List<Object[]> findTop3Popular() {
        String hql = "SELECT p AS product, SUM(oi.quantity) AS totalQty " +
                "FROM OrderItem oi " +
                "JOIN oi.product p " +
                "JOIN oi.order o " +
                "WHERE o.orderStatus = 'Completed' " +
                "GROUP BY p.productId " +
                "ORDER BY SUM(oi.quantity) DESC";

        TypedQuery<Object[]> query = entityManager.createQuery(hql, Object[].class);
        query.setMaxResults(3);
        return query.getResultList();
    }
}