package org.example.onlinemart.dao;

import org.example.onlinemart.entity.Watchlist;

import java.util.List;

public interface WatchlistDAO {
    void save(Watchlist watchlist);
    void delete(Watchlist watchlist);
    Watchlist findById(int watchlistId);
    Watchlist findByUserAndProduct(int userId, int productId);
    List<Watchlist> findByUserId(int userId);
    List<Watchlist> findAll();
}
