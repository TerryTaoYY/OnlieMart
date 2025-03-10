package org.example.onlinemart.dao.impl;

import org.example.onlinemart.dao.WatchlistDAO;
import org.example.onlinemart.entity.Watchlist;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class WatchlistDAOImpl implements WatchlistDAO {
    private final SessionFactory sessionFactory;

    public WatchlistDAOImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void save(Watchlist watchlist) {
        sessionFactory.getCurrentSession().save(watchlist);
    }

    @Override
    public void delete(Watchlist watchlist) {
        sessionFactory.getCurrentSession().delete(watchlist);
    }

    @Override
    public Watchlist findById(int watchlistId) {
        return sessionFactory.getCurrentSession().get(Watchlist.class, watchlistId);
    }

    @Override
    public Watchlist findByUserAndProduct(int userId, int productId) {
        Session session = sessionFactory.getCurrentSession();
        Criteria criteria = session.createCriteria(Watchlist.class, "w");
        criteria.createAlias("w.user", "u");
        criteria.createAlias("w.product", "p");
        criteria.add(Restrictions.eq("u.userId", userId));
        criteria.add(Restrictions.eq("p.productId", productId));
        return (Watchlist) criteria.uniqueResult();
    }

    @Override
    public List<Watchlist> findByUserId(int userId) {
        Session session = sessionFactory.getCurrentSession();
        Criteria criteria = session.createCriteria(Watchlist.class, "w");
        criteria.createAlias("w.user", "u");
        criteria.add(Restrictions.eq("u.userId", userId));
        return criteria.list();
    }

    @Override
    public List<Watchlist> findAll() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM Watchlist", Watchlist.class)
                .list();
    }
}
