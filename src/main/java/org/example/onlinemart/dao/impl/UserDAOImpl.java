package org.example.onlinemart.dao.impl;

import org.example.onlinemart.dao.UserDAO;
import org.example.onlinemart.entity.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class UserDAOImpl implements UserDAO {

    private final SessionFactory sessionFactory;

    public UserDAOImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(User user) {
        sessionFactory.getCurrentSession().save(user);
    }

    @Override
    public void update(User user) {
        sessionFactory.getCurrentSession().update(user);
    }

    @Override
    public User findById(int userId) {
        return sessionFactory.getCurrentSession().get(User.class, userId);
    }

    @Override
    public User findByUsername(String username) {
        Session session = sessionFactory.getCurrentSession();
        Query<User> query = session.createQuery("FROM User u WHERE u.username = :username", User.class);
        query.setParameter("username", username);
        return query.uniqueResult();
    }

    @Override
    public User findByEmail(String email) {
        Session session = sessionFactory.getCurrentSession();
        Query<User> query = session.createQuery("FROM User u WHERE u.email = :email", User.class);
        query.setParameter("email", email);
        return query.uniqueResult();
    }

    @Override
    public List<User> findAll() {
        Session session = sessionFactory.getCurrentSession();
        return session.createQuery("FROM User", User.class).list();
    }
}