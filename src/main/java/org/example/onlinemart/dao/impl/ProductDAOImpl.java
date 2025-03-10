package org.example.onlinemart.dao.impl;

import org.example.onlinemart.dao.ProductDAO;
import org.example.onlinemart.entity.Product;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class ProductDAOImpl implements ProductDAO {
    private final SessionFactory sessionFactory;

    public ProductDAOImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(Product product) {
        sessionFactory.getCurrentSession().save(product);
    }

    @Override
    public void update(Product product) {
        sessionFactory.getCurrentSession().update(product);
    }

    @Override
    public Product findById(int productId) {
        return sessionFactory.getCurrentSession().get(Product.class, productId);
    }

    @Override
    public List<Product> findAll() {
        Session session = sessionFactory.getCurrentSession();
        return session.createQuery("FROM Product", Product.class).list();
    }

    @Override
    public List<Product> findAllInStock() {
        Session session = sessionFactory.getCurrentSession();
        String hql = "FROM Product p WHERE p.stock > 0";
        return session.createQuery(hql, Product.class).list();
    }
}
